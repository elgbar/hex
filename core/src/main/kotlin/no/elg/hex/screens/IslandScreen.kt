package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.input.BasicInputProcessor
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.input.MapEditorInputProcessor
import no.elg.hex.renderer.OutlineRenderer
import no.elg.hex.renderer.SpriteRenderer
import no.elg.hex.renderer.VerticesRenderer
import no.elg.hex.screens.LevelSelectScreen.getIslandFile
import no.elg.island.Island

/**
 * @author Elg
 */
class IslandScreen(val island: Island) : AbstractScreen() {

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

  val basicInputProcessor: BasicInputProcessor by lazy { BasicInputProcessor(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  private val verticesRenderer = VerticesRenderer(this)
  private val outlineRenderer = OutlineRenderer(this)
  private val spriteRenderer = SpriteRenderer(this)

  override fun show() {
    Hex.inputMultiplexer.addProcessor(basicInputProcessor)
    Hex.inputMultiplexer.addProcessor(inputProcessor)
  }

  override fun render(delta: Float) {
    camera.update()

    verticesRenderer.frameUpdate()
    outlineRenderer.frameUpdate()
    spriteRenderer.frameUpdate()

    if (Hex.args.debug) {
      debugRenderer.frameUpdate()
    }
    frameUpdatable.frameUpdate()
  }


  fun saveIsland(): Boolean {
    val file = getIslandFile(basicInputProcessor.saveSlot)
    val name = file.name()

    if (!island.validate()) {
      Gdx.app.log("SAVE", "Island failed validation")
      return false
    }

    if (file.isDirectory) {
      Gdx.app.log("SAVE", "Failed to save island the name '$name' as the resulting file will be a directory.")
      return false
    }
    file.writeString(island.serialize(), false)
    Gdx.app.log("SAVE", "Successfully saved island '$name'")
    return true
  }

  override fun dispose() {
    super.dispose()
    Hex.inputMultiplexer.removeProcessor(basicInputProcessor)
    Hex.inputMultiplexer.removeProcessor(inputProcessor)
  }

  override fun hide() {
    dispose()
  }
}
