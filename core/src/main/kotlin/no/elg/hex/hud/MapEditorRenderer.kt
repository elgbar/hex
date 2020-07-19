package no.elg.hex.hud

import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.BOTTOM
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.input.MapEditorInput.MAX_BRUSH_SIZE
import no.elg.hex.input.MapEditorInput.MIN_BRUSH_SIZE
import no.elg.hex.input.MapEditorInput.brushRadius
import no.elg.hex.input.MapEditorInput.opaquenessEditor
import no.elg.hex.input.MapEditorInput.selectedTeam
import no.elg.hex.input.MapEditorInput.teamEditor
import no.elg.hex.input.editor.Editor.Companion.editorText
import no.elg.hex.input.editor.TeamEditor
import no.elg.hex.input.editor.TeamEditor.Disabled

/**
 * @author Elg
 */
object MapEditorRenderer : FrameUpdatable {

  var showHelp = true

  override fun frameUpdate() {
    ScreenRenderer.drawAll(
      ScreenText("Brush radius: ", next = validatedText(brushRadius, MIN_BRUSH_SIZE, MAX_BRUSH_SIZE, color = Color.YELLOW)),
      when (teamEditor) {
        TeamEditor.`Set team` -> ScreenText("Selected team: ", next = ScreenText(selectedTeam.name, color = Color.YELLOW))
        TeamEditor.`Randomize team` -> ScreenText("Selected team: ", next = ScreenText("random", color = Color.PURPLE))
        Disabled -> emptyText()
      },
      emptyText(),
      ScreenText("Opaqueness editor: ", next = editorText(opaquenessEditor)),
      ScreenText("Team editor: ", next = editorText(teamEditor)),
      position = TOP_RIGHT
    )

    if (showHelp) {
      ScreenRenderer.drawAll(
        ScreenText("=== Map editor keys ===", color = Color.SALMON),
        ScreenText("F1 to hide this help text"),
        emptyText(),
        ScreenText("Left click on a hexagon to remove it"),
        ScreenText("Shift left click to edit hexagons in a radius of $brushRadius"),
        emptyText(),
        ScreenText("W/up/pgUp to increase radius of brush"),
        ScreenText("S/down/pgDown to decrease radius of brush"),
        ScreenText("Q to iterate through teams"),
        emptyText(),
        ScreenText("1 to iterate through opaqueness editors"),
        ScreenText("2 to iterate through team editors"),
        emptyText(),
        ScreenText("F5 to quick save island"),
        ScreenText("F9 to quick load island"),
        ScreenText("CTRL+S to save current island to disk (TODO)", color = Color.LIGHT_GRAY),
        ScreenText("CTRL+L to load island from disk (TODO)", color = Color.LIGHT_GRAY),
        position = BOTTOM
      )
    }
  }
}
