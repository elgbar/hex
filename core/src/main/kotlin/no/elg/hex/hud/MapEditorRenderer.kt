package no.elg.hex.hud

import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.input.MapEditorInputProcessor
import no.elg.hex.input.editor.Editor.Companion.editorText
import no.elg.hex.input.editor.PieceEditor
import no.elg.hex.input.editor.TeamEditor
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.screens.MapEditorScreen.Companion.MAX_BRUSH_SIZE
import no.elg.hex.screens.MapEditorScreen.Companion.MIN_BRUSH_SIZE

/** @author Elg */
class MapEditorRenderer(
    private val mapEditorScreen: MapEditorScreen,
    private val mapEditorInputProcessor: MapEditorInputProcessor
) : FrameUpdatable {

  companion object {
    private val title = ScreenText("=== Map editor keys ===", color = Color.SALMON)

    val shownHelp =
        arrayOf(
            title,
            ScreenText("F1 to hide this help text"),
            ScreenText("Holding SHIFT will reverse iteration order, unless otherwise stated"),
            emptyText(),
            ScreenText("Left click on a hexagon to remove it"),
            ScreenText("Shift left click to edit hexagons in a radius of \$brushRadius"),
            emptyText(),
            ScreenText("W/up/pgUp to increase radius of brush"),
            ScreenText("S/down/pgDown to decrease radius of brush"),
            ScreenText("Q to iterate through teams"),
            ScreenText("A to iterate through pieces"),
            emptyText(),
            ScreenText("1 to iterate through opaqueness editors"),
            ScreenText("2 to iterate through team editors"),
            ScreenText("3 to iterate through piece editors"),
            emptyText(),
            ScreenText("Ctrl+C recalculate capitals"),
            emptyText(),
            ScreenText("F5 to quick save island"),
            ScreenText("F9 to quick load island"),
            ScreenText("Ctrl+O to output current island to disk"),
            ScreenText("Ctrl+R to read island from disk"))

    val hiddenHelp = arrayOf(title, ScreenText("F1 to show help text"))
  }

  override fun frameUpdate() {
    with(mapEditorInputProcessor) {
      val editorInfo =
          when (editor) {
            is TeamEditor ->
                ScreenText(
                    "Selected team: ",
                    next =
                        when (editor as TeamEditor) {
                          is TeamEditor.`Set team` ->
                              ScreenText(selectedTeam.name, color = Color.YELLOW)
                          is TeamEditor.`Randomize team` ->
                              ScreenText("random", color = Color.PURPLE)
                        })
            is PieceEditor ->
                ScreenText(
                    "Selected piece: ",
                    next =
                        when (editor as PieceEditor) {
                          is PieceEditor.`Set piece` ->
                              nullCheckedText(selectedPiece.simpleName, color = Color.YELLOW)
                          is PieceEditor.`Randomize piece` ->
                              ScreenText("random", color = Color.PURPLE)
                        })
            else -> emptyText()
          }

      ScreenRenderer.drawAll(
          ScreenText(
              "Brush radius: ",
              next =
                  validatedText(brushRadius, MIN_BRUSH_SIZE, MAX_BRUSH_SIZE, color = Color.YELLOW)),
          ScreenText(
              "Island id: ",
              next = validatedText(mapEditorScreen.id, 0, Int.MAX_VALUE, color = Color.YELLOW)),
          ScreenText("Editor: ", next = editorText(editor)),
          emptyText(),
          editorInfo,
          position = TOP_RIGHT)

    //      if (showHelp) {
    //        ScreenRenderer.drawAll(*shownHelp, position = BOTTOM_LEFT)
    //      } else {
    //        ScreenRenderer.drawAll(*hiddenHelp, position = BOTTOM_LEFT)
    //      }
    }
  }
}
