package no.elg.hex

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Application.LOG_INFO
import com.badlogic.gdx.Application.LOG_NONE
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.GL20
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.kotcrab.vis.ui.VisUI
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import no.elg.hex.Settings.MSAA_SAMPLES_PATH
import no.elg.hex.audio.MusicHandler
import no.elg.hex.event.Events
import no.elg.hex.hud.GLProfilerRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.hud.ScreenRenderer
import no.elg.hex.input.GlobalInputProcessor
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
import no.elg.hex.util.islandPreferences
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
  @Deprecated("Use mapper, and compress the output")
  val smileMapper: SmileMapper =
    SmileMapper.builder().apply {
      addModule(kotlinModule())
      configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }.build()

  @JvmStatic
  val mapper = jsonMapper {
    addModule(kotlinModule())
    defaultPropertyInclusion(JsonInclude.Value.construct(NON_DEFAULT, NON_DEFAULT))
    addMixIn(CubeCoordinate::class.java, CubeCoordinateMixIn::class.java)
    addModule(SimpleModule().also { module -> module.setDeserializerModifier(HexagonDataDeserializerModifier()) })
    configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
    configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true)
  }

  private const val RENDER_FAILED_THRESHOLD = 3

  val AA_BUFFER_CLEAR =
    lazy { if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0 }

  lateinit var args: ApplicationArgumentsParser

  private var assets0: Assets? = null
  val assets: Assets get() = assets0 ?: error("Assets not initialized")

  val asyncThread: AsyncExecutorDispatcher get() = internalAsyncThread ?: error("Async thread not initialized")

  private var internalAsyncThread: AsyncExecutorDispatcher? = null
    set(value) {
      field?.dispose()
      field = value
    }

  var paused = false
    private set

  private var renderFailures = 0

  val assetsAvailable: Boolean get() = assets0 != null

  val inputMultiplexer = InputMultiplexer()

  lateinit var platform: Platform

  lateinit var launchPreference: Preferences
  var audioDisabled: Boolean = true

  val debugStage by lazy { trace || args.`stage-debug` }
  val debug by lazy { args.debug || args.trace }
  val trace by lazy { args.trace }
  val mapEditor by lazy { args.mapEditor }
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
        inputMultiplexer.addProcessor(GlobalInputProcessor)
        Gdx.input.setOnscreenKeyboardVisible(false)
        Gdx.graphics.isContinuousRendering = false
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
        Events.clear(true)

        Gdx.app.debug("SCREEN") { "Loading new screen ${value::class.simpleName}" }
        value.show()
        value.resize(Gdx.graphics.width, Gdx.graphics.height)
        // Keep this last
        field = value
        value.afterShown()
      }
      Gdx.graphics.requestRendering()
    }

  val music: MusicHandler = MusicHandler()

  override fun create() {
    if (args.`reset-all`) {
      islandPreferences.clear()
      Settings.resetSettings.onResetConfirmed()
    }

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
          val bySettings = "${launchPreference.getBoolean(Settings.VSYNC_PATH)} (by settings)"
          val byPlatform = "${platform.vsync} (by platform)"
          "VSYNC: $bySettings / $byPlatform"
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
      renderFailures = 0
    } catch (e: Throwable) {
      e.printStackTrace()
      MessagesRenderer.publishError("Threw exception when rending frame ${Gdx.graphics.frameId}: ${e::class.simpleName} : ${e.message}", 120f)
      if (++renderFailures > RENDER_FAILED_THRESHOLD) {
        Gdx.app.error("RENDER", "Too many render failures, exiting")
        Gdx.app.postRunnable {
          screen = LevelSelectScreen()
        }
      }
    }
  }

  override fun resume() {
    paused = false
    renderFailures = 0
    resetClearColor()
    ScreenRenderer.resume()

    internalAsyncThread = newSingleThreadAsyncContext()

    if (assets0 == null) {
      assets0 = Assets()
      Gdx.app.postRunnable {
        assets.loadAssets()
      }
    }
    Gdx.app.postRunnable {
      // must be last
      assets.finishMain()
    }
  }

  override fun pause() {
    paused = true
    inputMultiplexer.clear()
    Gdx.app.postRunnable {
      screen = SplashScreen(screen)
    }
    assets.unready()
    internalAsyncThread = null
    ScreenRenderer.dispose()
  }

  override fun resize(width: Int, height: Int) {
    if (paused || width <= 0 || height <= 0) {
      return
    }
    ScreenRenderer.resize(width, height)
    screen.resize(width, height)
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