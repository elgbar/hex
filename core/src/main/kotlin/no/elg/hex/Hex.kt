package no.elg.hex

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.VisUI.SkinScale.X1
import com.kotcrab.vis.ui.VisUI.SkinScale.X2
import no.elg.hex.hexagon.renderer.OutlineRenderer
import no.elg.hex.hexagon.renderer.VerticesRenderer
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.hud.ScreenRenderer
import no.elg.hex.input.BasicInputHandler
import no.elg.hex.input.MapEditorInput
import no.elg.hex.map.Map


object Hex : ApplicationAdapter() {

  val map = Map(99)
  val camera: OrthographicCamera = OrthographicCamera()

  private val AA_BUFFER_CLEAR = lazy { if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0 }

  lateinit var args: ApplicationParser


  override fun create() {
    require(this::args.isInitialized) { "An instance of ApplicationParser must be set before calling create()" }

    val inputMultiplexer = InputMultiplexer()
    Gdx.input.inputProcessor = inputMultiplexer
    inputMultiplexer.addProcessor(BasicInputHandler)
    if (args.mapEditor) {
      inputMultiplexer.addProcessor(MapEditorInput)
    }

    if (BasicInputHandler.scale > 1) {
      VisUI.load(X2)
    } else {
      VisUI.load(X1)
    }
    BasicInputHandler.resetCamera()
    val backgroundColor = Color.valueOf("#172D62")
    Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, 1f)
  }

  override fun render() {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or AA_BUFFER_CLEAR.value)

    camera.update()

    VerticesRenderer.frameUpdate()
    OutlineRenderer.frameUpdate()

    if (args.debug) {
      DebugInfoRenderer.frameUpdate()
    } else if (args.mapEditor) {
      MapEditorRenderer.frameUpdate()
    }
  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(true, width.toFloat(), height.toFloat())
    ScreenRenderer.resize(width, height)
    BasicInputHandler.resetCamera()
  }
}
