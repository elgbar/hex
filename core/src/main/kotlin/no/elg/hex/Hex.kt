package no.elg.hex

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.VisUI.SkinScale.X1
import com.kotcrab.vis.ui.VisUI.SkinScale.X2
import no.elg.hex.hud.ScreenRenderer
import no.elg.hex.jackson.mixin.CubeCoordinateMixIn
import no.elg.hex.screens.AbstractScreen
import no.elg.hex.screens.SplashScreen
import org.hexworks.mixite.core.api.CubeCoordinate
import java.awt.Toolkit


object Hex : ApplicationAdapter() {

  @JvmStatic
  val mapper = jacksonObjectMapper().also {
    it.addMixIn(CubeCoordinate::class.java, CubeCoordinateMixIn::class.java)
  }

  val AA_BUFFER_CLEAR = lazy { if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0 }

  lateinit var args: ApplicationArgumentsParser
  lateinit var assets: Assets
    private set

  val inputMultiplexer = InputMultiplexer()

  val scale: Int = if (Toolkit.getDefaultToolkit().screenSize.width > 2560) 2 else 1

  var screen: Screen = SplashScreen
    set(value) {
      val old = field
      Gdx.app.log("SCREEN", "Unloading old screen ${old::class.simpleName}")
      if (old is AbstractScreen) {
        old.hide()
      }
      Gdx.app.log("SCREEN", "Loading new screen ${value::class.simpleName}")
      if (value is AbstractScreen) {
        value.show()
      }
      field = value
    }

  override fun create() {
    require(this::args.isInitialized) { "An instance of ApplicationParser must be set before calling create()" }

    val backgroundColor = Color.valueOf("#172D62")
    Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, 1f)

    assets = Assets()

    Gdx.input.inputProcessor = inputMultiplexer

    if (scale > 1) {
      VisUI.load(X2)
    } else {
      VisUI.load(X1)
    }

    //must be last
    assets.finishMain()
  }

  override fun render() {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or Hex.AA_BUFFER_CLEAR.value)
    screen.render(Gdx.graphics.deltaTime)
  }

  override fun resize(width: Int, height: Int) {
    ScreenRenderer.resize(width, height)
    screen.resize(width, height)
  }
}
