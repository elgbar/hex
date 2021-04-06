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
import com.badlogic.gdx.graphics.GL20
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kotcrab.vis.ui.VisUI
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import no.elg.hex.Settings.MSAA_SAMPLES_PATH
import no.elg.hex.hud.GLProfilerRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.hud.ScreenRenderer
import no.elg.hex.jackson.mixin.CubeCoordinateMixIn
import no.elg.hex.screens.AbstractScreen
import no.elg.hex.screens.SettingsScreen
import no.elg.hex.screens.SplashScreen
import no.elg.hex.screens.TutorialScreen
import no.elg.hex.util.LOG_TRACE
import no.elg.hex.util.resetHdpi
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.CubeCoordinate

object Hex : ApplicationAdapter() {

  const val LAUNCH_PREF = "launchPref"

  @JvmStatic
  val mapper =
    jacksonObjectMapper().also {
      it.addMixIn(CubeCoordinate::class.java, CubeCoordinateMixIn::class.java)
    }

  val AA_BUFFER_CLEAR =
    lazy { if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0 }

  lateinit var args: ApplicationArgumentsParser

  @Suppress("LibGDXStaticResource")
  lateinit var assets: Assets
    private set

  @Suppress("LibGDXStaticResource")
  lateinit var asyncThread: AsyncExecutorDispatcher
    private set

  var paused = false
    private set

  val assetsAvailable: Boolean get() = Hex::assets.isInitialized

  val inputMultiplexer = InputMultiplexer()

  lateinit var tutorialScreen: TutorialScreen
    private set

  lateinit var settingsScreen: SettingsScreen
    private set

  lateinit var launchPreference: Preferences

  val debug by lazy { args.debug || args.trace }
  val trace by lazy { args.trace }
  val scale by lazy {
    if (args.scale <= 0) Assets.nativeScale else args.scale
  }
  val backgroundColor: Color by lazy { if (args.mapEditor) Color.valueOf("#60173F") else Color.valueOf("#172D62") }

  var screen: AbstractScreen = SplashScreen
    set(value) {
      val old = field
      Gdx.app.trace("SCREEN", "Unloading old screen ${old::class.simpleName}")
      old.hide()

      // clean up any mess the previous screen have made
      inputMultiplexer.clear()
      Gdx.input.setOnscreenKeyboardVisible(false)

      Gdx.app.debug("SCREEN", "Loading new screen ${value::class.simpleName}")
      value.show()
      value.resize(Gdx.graphics.width, Gdx.graphics.height)
      field = value
    }

  override fun create() {
    paused = false

    try {
      require(this::args.isInitialized) { "An instance of ApplicationParser must be set before calling create()" }

      Gdx.app.logLevel =
        when {
          args.silent -> LOG_NONE
          args.trace -> LOG_TRACE
          args.debug -> LOG_DEBUG
          else -> LOG_INFO
        }

      KtxAsync.initiate()
      Gdx.app.debug("SYS", "App backend ${Gdx.app.type}")
      Gdx.app.debug("SYS", "Max pointers ${Gdx.input.maxPointers}")
      Gdx.app.debug("SYS", "MSAA ${launchPreference.getInteger(MSAA_SAMPLES_PATH)}")

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

  override fun render() {
    try {

      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or AA_BUFFER_CLEAR.value)
      screen.render(Gdx.graphics.deltaTime)
      ScreenRenderer.camera.resetHdpi()
      MessagesRenderer.frameUpdate()
      GLProfilerRenderer.frameUpdate()
    } catch (e: Throwable) {
      e.printStackTrace()
      MessagesRenderer.publishError("Threw when rending frame ${Gdx.graphics.frameId}: ${e::class.simpleName}", 600f)
      dispose()
      create()
    }
  }

  override fun resume() {
    paused = false
    setClearColorAlpha(1f)
    ScreenRenderer.resume()

    asyncThread = newSingleThreadAsyncContext()

    assets = Assets()
    screen = SplashScreen

    assets.loadAssets()

    tutorialScreen = TutorialScreen()
    settingsScreen = SettingsScreen()

    // must be last
    assets.finishMain()
  }

  override fun pause() {
    paused = true
    SplashScreen.nextScreen = screen
    screen = SplashScreen
    inputMultiplexer.clear()
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or AA_BUFFER_CLEAR.value)
    assets.dispose()
    asyncThread.dispose()
    ScreenRenderer.dispose()
    settingsScreen.dispose()
    tutorialScreen.dispose()
  }

  override fun resize(width: Int, height: Int) {
    ScreenRenderer.resize(width, height)
    screen.resize(width, height)
  }

  override fun dispose() {
    paused = true
    try {
      pause()
      VisUI.dispose()
      screen.dispose()
      assets.dispose()
    } catch (e: Exception) {
    }
  }

  fun setClearColorAlpha(alpha: Float) {
    Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, alpha)
  }
}
