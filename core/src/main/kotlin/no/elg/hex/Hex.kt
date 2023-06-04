package no.elg.hex

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Application.LOG_INFO
import com.badlogic.gdx.Application.LOG_NONE
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.kotcrab.vis.ui.VisUI
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import no.elg.hex.Settings.MSAA_SAMPLES_PATH
import no.elg.hex.event.Events
import no.elg.hex.hud.GLProfilerRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.hud.ScreenRenderer
import no.elg.hex.island.Island
import no.elg.hex.jackson.mixin.CubeCoordinateMixIn
import no.elg.hex.jackson.serialization.HexagonDataDeserializerModifier
import no.elg.hex.platform.Platform
import no.elg.hex.platform.PlatformType
import no.elg.hex.screens.AbstractScreen
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.screens.SplashScreen
import no.elg.hex.util.LOG_TRACE
import no.elg.hex.util.debug
import no.elg.hex.util.info
import no.elg.hex.util.logLevelToName
import no.elg.hex.util.reportTiming
import no.elg.hex.util.resetHdpi
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.CubeCoordinate
import kotlin.system.exitProcess

@Suppress("GDXKotlinStaticResource")
object Hex : ApplicationAdapter() {

  const val LAUNCH_PREF = "launchPref"

  @JvmStatic
  val mapper = jsonMapper {
    addModule(kotlinModule())
    configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
    serializationInclusion(JsonInclude.Include.NON_DEFAULT)
    addMixIn(CubeCoordinate::class.java, CubeCoordinateMixIn::class.java)
    addModule(SimpleModule().also { module -> module.setDeserializerModifier(HexagonDataDeserializerModifier()) })
  }

  val AA_BUFFER_CLEAR =
    lazy { if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0 }

  lateinit var args: ApplicationArgumentsParser

  lateinit var assets: Assets
    private set

  lateinit var asyncThread: AsyncExecutorDispatcher
    private set

  var paused = false
    private set

  val assetsAvailable: Boolean get() = Hex::assets.isInitialized

  val inputMultiplexer = InputMultiplexer()

  lateinit var platform: Platform

  lateinit var launchPreference: Preferences
  var audioDisabled: Boolean = true

  val debugStage by lazy { trace || args.`stage-debug` }
  val debug by lazy { args.debug || args.trace }
  val trace by lazy { args.trace }
  val scale by lazy {
    if (args.scale <= 0) Assets.nativeScale else args.scale
  }
  val backgroundColor: Color by lazy { if (args.mapEditor) Color.valueOf("#60173F") else Color.valueOf("#172D62") }

  val island: Island? get() = (screen as? PreviewIslandScreen)?.island

  var screen: AbstractScreen = SplashScreen(LevelSelectScreen())
    set(value) {
      reportTiming("change screen to ${field::class.simpleName}") {
        val old = field
        Gdx.app.trace("SCREEN") { "Unloading old screen ${old::class.simpleName}" }
        old.hide()

        // clean up any mess the previous screen have made
        inputMultiplexer.clear()
        Gdx.input.setOnscreenKeyboardVisible(false)
        Events.clear()

        Gdx.app.debug("SCREEN", "Loading new screen ${value::class.simpleName}")
        value.show()
        value.resize(Gdx.graphics.width, Gdx.graphics.height)
        // Keep this last
        field = value
      }
      Gdx.graphics.requestRendering()
    }

  override fun create() {
    Gdx.app.logLevel =
      when {
        args.silent -> LOG_NONE
        args.trace -> LOG_TRACE
        args.debug -> LOG_DEBUG
        else -> LOG_INFO
      }
    reportTiming("create") {
      Gdx.graphics.isContinuousRendering = false
      paused = false

      try {
        require(this::args.isInitialized) { "An instance of ApplicationParser must be set before calling create()" }

        KtxAsync.initiate()
        Gdx.app.info("SYS") { "Version: ${platform.version}" }
        Gdx.app.info("SYS") { "App log level: ${logLevelToName(Gdx.app.logLevel)}" }
        Gdx.app.debug("SYS") { "App backend: ${Gdx.app.type}" }
        Gdx.app.debug("SYS") { "App version: ${Gdx.app.version}" }
        Gdx.app.debug("SYS") { "Max pointers: ${Gdx.input.maxPointers}" }
        Gdx.app.debug("SYS") { "GraphicsType: ${Gdx.graphics.type}" }
        Gdx.app.debug("SYS") { "GL version: ${Gdx.graphics.glVersion.debugVersionString}" }
        Gdx.app.debug("SYS") { "MSAA: ${launchPreference.getInteger(MSAA_SAMPLES_PATH, -1)}" }
        Gdx.app.debug("SYS") {
          "VSYNC: ${
            Gdx.app.graphics.let {
              if (it is LwjglGraphics) {
                "${
                  it::class.java.getDeclaredField("vsync").also { field -> field.isAccessible = true }.get(it)
                } (by field)"
              } else {
                "${launchPreference.getBoolean(Settings.VSYNC_PATH)} (by settings)"
              }
            }
          }"
        }

        Gdx.input.inputProcessor = inputMultiplexer
        Gdx.input.setCatchKey(Keys.BACK, true)

        if (args.profile) {
          GLProfilerRenderer.enable()
        }

        resume()
      } catch (e: Throwable) {
        e.printStackTrace()
        MessagesRenderer.publishError("Threw when loaded: $e", 600f)
      }
    }
  }

  override fun render() {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or AA_BUFFER_CLEAR.value)
    if (paused) {
      return
    }
    try {
      screen.render(Gdx.graphics.deltaTime)
      ScreenRenderer.camera.resetHdpi()
      MessagesRenderer.frameUpdate()
      GLProfilerRenderer.frameUpdate()
    } catch (e: Throwable) {
      e.printStackTrace()
      MessagesRenderer.publishError("Threw exception when rending frame ${Gdx.graphics.frameId}: ${e::class.simpleName}", 600f)

      Gdx.app.postRunnable {
        screen = LevelSelectScreen()
      }
    }
  }

  override fun resume() {
    paused = false
    resetClearColor()
    ScreenRenderer.resume()

    asyncThread = newSingleThreadAsyncContext()

    assets = Assets()
    Gdx.app.postRunnable {
      assets.loadAssets()

      // must be last
      assets.finishMain()
    }
  }

  override fun pause() {
    paused = true
    inputMultiplexer.clear()
    Gdx.app.postRunnable {
      screen = SplashScreen(screen)
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or AA_BUFFER_CLEAR.value)
    }
    assets.dispose()
    asyncThread.dispose()
    ScreenRenderer.dispose()
    VisUI.dispose(false)
  }

  override fun resize(width: Int, height: Int) {
    if (width > 0 && height > 0) {
      ScreenRenderer.resize(width, height)
      screen.resize(width, height)
    }
  }

  override fun dispose() {
    try {
      VisUI.dispose(true)
      screen.dispose()
      if (platform.type == PlatformType.DESKTOP) {
        exitProcess(0)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun resetClearColor() {
    setClearColorAlpha(1f)
  }

  fun setClearColorAlpha(alpha: Float) {
    Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, alpha)
  }
}