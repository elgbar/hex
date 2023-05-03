package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Buttons
import no.elg.hex.input.editor.EditMetadata
import no.elg.hex.input.editor.Editor
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.util.calculateHexagonsWithinRadius
import no.elg.hex.util.getData
import no.elg.hex.util.trace
import no.elg.hex.util.withData

/** @author Elg */
class MapEditorInputProcessor(private val screen: MapEditorScreen) : AbstractInput() {

  private var currentMetadata: EditMetadata? = null
  private var metaEditor: Editor? = null

  private val isEditorChanged: Boolean
    get() {
      val metadata = currentMetadata
      return metadata == null || (metadata.selectedPiece != screen.selectedPiece || metadata.selectedTeam != screen.selectedTeam || screen.editor != metaEditor)
    }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (button == Buttons.LEFT) {
      val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false

      val island = screen.island
      val clickedHexagonData = island.getData(cursorHex).copy()
      metaEditor = screen.editor
      currentMetadata = EditMetadata(
        clickedHexagon = cursorHex,
        clickedHexagonData = clickedHexagonData,
        island = island,
        selectedPiece = screen.selectedPiece,
        selectedTeam = screen.selectedTeam
      )
      return true
    }
    return false
  }

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    currentMetadata = null
    return false
  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    val metadata = currentMetadata
    if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && metadata != null) {
      if (isEditorChanged) {
        currentMetadata = null
        return false
      }
      val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false

      val editor = screen.editor
      metaEditor = editor
      val hexagons = screen.island.calculateHexagonsWithinRadius(cursorHex, screen.brushRadius - 1)
      hexagons.withData(metadata.island, excludeInvisible = false) { hex, data ->
        Gdx.app.trace("Editor") { "Editing ${hex.cubeCoordinate.toAxialKey()}" }
        editor.edit(hex, data, metadata)
      }
      editor.postEdit(metadata)
      return true
    }
    return false
  }
}