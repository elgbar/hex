package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.ScreenText
import no.elg.hex.input.BasicIslandInputProcessor
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.input.MapEditorInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.renderer.OutlineRenderer
import no.elg.hex.renderer.SpriteRenderer
import no.elg.hex.renderer.VerticesRenderer
import no.elg.hex.screens.LevelSelectScreen.getIslandFileName

/**
 * @author Elg
 */
class IslandScreen(
  val id: Int,
  val island: Island,
  private val renderHud: Boolean = true
) : AbstractScreen() {

  val inputProcessor: InputProcessor by lazy {
    if (Hex.args.mapEditor) {
      MapEditorInputProcessor(this)
    } else {
      GameInputProcessor(this)
    }
  }

  private val frameUpdatable: FrameUpdatable by lazy {
    if (Hex.args.mapEditor) {
      MapEditorRenderer(this, inputProcessor as MapEditorInputProcessor)
    } else {
      GameInfoRenderer(this, inputProcessor as GameInputProcessor)
    }
  }

  val basicIslandInputProcessor: BasicIslandInputProcessor by lazy { BasicIslandInputProcessor(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  private val verticesRenderer = VerticesRenderer(this)
  private val outlineRenderer = OutlineRenderer(this)
  private val spriteRenderer = SpriteRenderer(this)


  override fun render(delta: Float) {
    camera.update()

    verticesRenderer.frameUpdate()
    outlineRenderer.frameUpdate()
    spriteRenderer.frameUpdate()

    if (renderHud) {
      if (Hex.args.debug || Hex.args.trace) {
        debugRenderer.frameUpdate()
      }
      frameUpdatable.frameUpdate()
    }
  }


  fun saveIsland(): Boolean {
    val file = Gdx.files.local(getIslandFileName(id))

    if (!island.validate()) {
      publishMessage(ScreenText("Island failed validation", color = Color.RED))
      return false
    }

    if (file.isDirectory) {
      publishMessage(ScreenText("Failed to save island the name '${file.name()}' as the resulting file will be a directory.", color = Color.RED))
      return false
    }
    return try {
      file.writeString(island.serialize(), false)
      publishMessage(ScreenText("Successfully saved island '${file.name()}'", color = Color.GREEN))
      true
    } catch (e: Throwable) {
      publishMessage(ScreenText("Failed to saved island '${file.name()}'", color = Color.RED))
      e.printStackTrace()
      false
    }
  }


  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    val data = island.grid.gridData

    val x = (data.gridWidth * data.hexagonWidth + data.gridWidth).toFloat() / 2f
    val y = (data.gridHeight * data.hexagonHeight + data.gridHeight).toFloat() / 2f

    camera.position.x = x
    camera.position.y = y
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.addProcessor(inputProcessor)
  }

  override fun hide() {
    dispose()
    island.select(null)
  }

  override fun dispose() {
    super.dispose()
    Hex.inputMultiplexer.removeProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.removeProcessor(inputProcessor)
  }
}
