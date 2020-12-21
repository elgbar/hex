package no.elg.hex

import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Application.LOG_INFO
import com.badlogic.gdx.Application.LOG_NONE
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kotcrab.vis.ui.VisUI
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.hud.ScreenRenderer
import no.elg.hex.jackson.mixin.CubeCoordinateMixIn
import no.elg.hex.screens.AbstractScreen
import no.elg.hex.screens.SplashScreen
import no.elg.hex.util.LOG_TRACE
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.CubeCoordinate

object Hex : ApplicationAdapter() {

  @JvmStatic
  val mapper =
    jacksonObjectMapper().also {
      it.addMixIn(CubeCoordinate::class.java, CubeCoordinateMixIn::class.java)
    }

  val AA_BUFFER_CLEAR =
    lazy { if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0 }

  lateinit var args: ApplicationArgumentsParser
  lateinit var assets: Assets
    private set

  val inputMultiplexer = InputMultiplexer()

  val debug by lazy { args.debug || args.trace }
  val trace by lazy { args.trace }
  val scale by lazy { if (args.scale <= 0) Assets.nativeScale else args.scale }
  private val backgroundColor: Color by lazy { if (args.mapEditor) Color.valueOf("#60173F") else Color.valueOf("#172D62") }

  var screen: AbstractScreen = SplashScreen
    set(value) {
      val old = field
      Gdx.app.trace("SCREEN", "Unloading old screen ${old::class.simpleName}")
      old.hide()
      Gdx.app.debug("SCREEN", "Loading new screen ${value::class.simpleName}")
      value.show()
      value.render(0f)
      value.resize(Gdx.graphics.width, Gdx.graphics.height)
      field = value
    }

  override fun create() {
    require(this::args.isInitialized) { "An instance of ApplicationParser must be set before calling create()" }

    Gdx.app.logLevel =
      when {
        args.silent -> LOG_NONE
        args.trace -> LOG_TRACE
        args.debug -> LOG_DEBUG
        else -> LOG_INFO
      }

    setClearColorAlpha(1f)

    assets = Assets()
    screen = SplashScreen
    assets.loadAssets()

    Gdx.input.inputProcessor = inputMultiplexer
    Gdx.input.setCatchKey(Keys.BACK, true)

    // must be last
    assets.finishMain()
  }

  override fun render() {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or AA_BUFFER_CLEAR.value)
    screen.render(Gdx.graphics.deltaTime)
    MessagesRenderer.frameUpdate()
  }

  override fun resize(width: Int, height: Int) {
    ScreenRenderer.resize(width, height)
    screen.resize(width, height)
  }

  override fun dispose() {
    VisUI.dispose()
  }

  fun setClearColorAlpha(alpha: Float) {
    Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, alpha)
  }
}
