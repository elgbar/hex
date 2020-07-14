package no.elg.hex

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.VisUI.SkinScale.X1
import com.kotcrab.vis.ui.VisUI.SkinScale.X2
import src.no.elg.hex.hexagon.renderer.OutlineRenderer
import src.no.elg.hex.hexagon.renderer.VerticesRenderer
import src.no.elg.hex.hud.HUDRenderer
import src.no.elg.hex.hud.ScreenRenderer
import src.no.elg.hex.input.InputHandler
import src.no.elg.hex.map.Map


object Hex : ApplicationAdapter() {

  val map = Map(25)
  val camera: OrthographicCamera = OrthographicCamera()

  private val AA_BUFFER_CLEAR = lazy { if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0 }

  override fun create() {
    Gdx.input.inputProcessor = InputHandler
    if (InputHandler.scale > 1) {
      VisUI.load(X2)
    } else {
      VisUI.load(X1)
    }
    InputHandler.resetCamera()
    val backgroundColor = Color.valueOf("#172D62")
    Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, 1f)
  }

  override fun render() {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or AA_BUFFER_CLEAR.value)

    camera.update()

    InputHandler.frameUpdate()

    VerticesRenderer.frameUpdate()
    OutlineRenderer.frameUpdate()

    HUDRenderer.frameUpdate()

  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(true, width.toFloat(), height.toFloat())
    ScreenRenderer.resize(width, height)
    InputHandler.resetCamera()
  }
}
