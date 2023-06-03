package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.input.editor.EditMetadata
import no.elg.hex.input.editor.Editor
import no.elg.hex.island.Island
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.util.calculateHexagonsWithinRadius
import no.elg.hex.util.getData
import no.elg.hex.util.trace
import no.elg.hex.util.withData
import org.hexworks.mixite.core.api.Hexagon

/** @author Elg */
class MapEditorInputProcessor(private val screen: MapEditorScreen) : AbstractInput() {

  private var currentMetadata: EditMetadata? = null
  private var metaEditor: Editor? = null

  private val isEditorChanged: Boolean
    get() {
      val metadata = currentMetadata
      return metadata == null || (metadata.selectedPiece != screen.selectedPiece || metadata.selectedTeam != screen.selectedTeam || screen.editor != metaEditor)
    }

  private fun postEdit(metadata: EditMetadata) {
    if (isEditorChanged) {
      currentMetadata = null
      return
    }
    val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return

    val editor = screen.editor
    metaEditor = editor
    val hexagons = screen.island.calculateHexagonsWithinRadius(cursorHex, screen.brushRadius - 1)
    hexagons.withData(metadata.island, excludeInvisible = false) { hex, data ->
      Gdx.app.trace("Editor") { "Editing ${hex.cubeCoordinate.toAxialKey()}" }
      editor.edit(hex, data, metadata)
    }
    editor.postEdit(metadata)
    screen.artbSpinner.setValue(Island.UNKNOWN_ROUNDS_TO_BEAT, true)
  }

  private fun setEditorMetadata(cursorHex: Hexagon<HexagonData>): EditMetadata {
    val island = screen.island
    val clickedHexagonData = island.getData(cursorHex).copy()
    metaEditor = screen.editor
    return EditMetadata(
      clickedHexagon = cursorHex,
      clickedHexagonData = clickedHexagonData,
      island = island,
      selectedPiece = screen.selectedPiece,
      selectedTeam = screen.selectedTeam
    ).also {
      currentMetadata = it
    }
  }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (button == Buttons.LEFT) {
      val cursorHex: Hexagon<HexagonData> = screen.basicIslandInputProcessor.cursorHex ?: return false
      val editorMetadata = setEditorMetadata(cursorHex)
      postEdit(editorMetadata)
      return true
    }
    return false
  }

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    currentMetadata = null
    return true
  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
      postEdit(currentMetadata ?: return false)
      return true
    }
    return false
  }
}