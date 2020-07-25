package no.elg.hex.hud

import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.BOTTOM
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.input.BasicInputHandler.saveSlot
import no.elg.hex.input.MapEditorInputHandler.MAX_BRUSH_SIZE
import no.elg.hex.input.MapEditorInputHandler.MIN_BRUSH_SIZE
import no.elg.hex.input.MapEditorInputHandler.brushRadius
import no.elg.hex.input.MapEditorInputHandler.opaquenessEditor
import no.elg.hex.input.MapEditorInputHandler.pieceEditor
import no.elg.hex.input.MapEditorInputHandler.selectedPiece
import no.elg.hex.input.MapEditorInputHandler.selectedTeam
import no.elg.hex.input.MapEditorInputHandler.teamEditor
import no.elg.hex.input.editor.Editor.Companion.editorText
import no.elg.hex.input.editor.PieceEditor
import no.elg.hex.input.editor.TeamEditor

/**
 * @author Elg
 */
object MapEditorRenderer : FrameUpdatable {

  var showHelp = false

  override fun frameUpdate() {
    ScreenRenderer.drawAll(
      ScreenText("Brush radius: ", next = validatedText(brushRadius, MIN_BRUSH_SIZE, MAX_BRUSH_SIZE, color = Color.YELLOW)),
      ScreenText("Save slot: ", next = validatedText(saveSlot, 0, Int.MAX_VALUE, color = Color.YELLOW)),
      ScreenText("Selected team: ", next = when (teamEditor) {
        TeamEditor.`Set team` -> ScreenText(selectedTeam.name, color = Color.YELLOW)
        TeamEditor.`Randomize team` -> ScreenText("random", color = Color.PURPLE)
        TeamEditor.Disabled -> ScreenText(selectedTeam.name, color = Color.LIGHT_GRAY)
      }),
      ScreenText("Selected piece: ", next = when (pieceEditor) {
        PieceEditor.`Set piece` -> nullCheckedText(selectedPiece.simpleName, color = Color.YELLOW)
        PieceEditor.`Randomize piece` -> ScreenText("random", color = Color.PURPLE)
        PieceEditor.Disabled -> nullCheckedText(selectedPiece.simpleName, color = Color.LIGHT_GRAY)
      }),
      emptyText(),
      ScreenText("Opaqueness editor: ", next = editorText(opaquenessEditor)),
      ScreenText("Team editor: ", next = editorText(teamEditor)),
      ScreenText("Piece editor: ", next = editorText(pieceEditor)),
      position = TOP_RIGHT
    )

    val title = ScreenText("=== Map editor keys ===", color = Color.SALMON)
    if (showHelp) {
      ScreenRenderer.drawAll(
        title,
        ScreenText("F1 to hide this help text"),
        ScreenText("Holding SHIFT will reverse iteration order, unless otherwise stated"),
        emptyText(),
        ScreenText("Left click on a hexagon to remove it"),
        ScreenText("Shift left click to edit hexagons in a radius of $brushRadius"),
        emptyText(),
        ScreenText("W/up/pgUp to increase radius of brush"),
        ScreenText("S/down/pgDown to decrease radius of brush"),
        ScreenText("Q to iterate through teams"),
        ScreenText("A to iterate through teams"),
        emptyText(),
        ScreenText("1 to iterate through opaqueness editors"),
        ScreenText("2 to iterate through team editors"),
        ScreenText("3 to iterate through piece editors"),
        emptyText(),
        ScreenText("F5 to quick save island"),
        ScreenText("F9 to quick load island"),
        ScreenText("RIGHT to increase save slot"),
        ScreenText("LEFT to decrease save slot"),
        ScreenText("CTRL+C to save current island to disk"),
        ScreenText("CTRL+V to load island from disk"),
        position = BOTTOM
      )
    } else {
      ScreenRenderer.drawAll(
        title,
        ScreenText("F1 to show help text"),
        position = BOTTOM
      )
    }
  }
}
