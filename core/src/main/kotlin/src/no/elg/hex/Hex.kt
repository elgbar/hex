package no.elg.hex

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.VisUI.SkinScale.X1
import com.kotcrab.vis.ui.VisUI.SkinScale.X2
import src.no.elg.hex.InputHandler
import src.no.elg.hex.hexagon.renderer.OutlineRenderer
import src.no.elg.hex.hexagon.renderer.VerticesRenderer
import src.no.elg.hex.rendrer.HUDRenderer
import src.no.elg.hex.rendrer.ScreenRenderer
import src.no.elg.hex.world.World


object Hex : ApplicationAdapter() {

  val world = World(10, 10)
  val camera: OrthographicCamera = OrthographicCamera()

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
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or
      if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0)

    camera.update()

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
