package no.elg.hex.hud

import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.BOTTOM
import no.elg.hex.input.MapEditorInput.MAX_BRUSH_SIZE
import no.elg.hex.input.MapEditorInput.MIN_BRUSH_SIZE
import no.elg.hex.input.MapEditorInput.brushRadius
import no.elg.hex.input.MapEditorInput.editMode

/**
 * @author Elg
 */
object MapEditorRenderer : FrameUpdatable {

  var showHelp = true

  override fun frameUpdate() {
    ScreenRenderer.drawAll(
      ScreenText("Brush radius: ", next = validatedText(brushRadius, MIN_BRUSH_SIZE, MAX_BRUSH_SIZE, color = Color.YELLOW)),
      ScreenText("Edit mode: ", next = ScreenText(editMode.name, color = Color.YELLOW))
    )

    if (showHelp) {
      ScreenRenderer.drawAll(
        ScreenText("=== Map editor keys ===", color = Color.SALMON),
        ScreenText("F1 to hide this help text"),
        emptyText(),
        ScreenText("Left click on a hexagon to remove it"),
        ScreenText("SHIFT+Left-click to ${editMode.name.toLowerCase()} hexagons in a radius of $brushRadius", color = Color.LIGHT_GRAY),
        emptyText(),
        ScreenText("SHIFT+W to increase radius of brush"),
        ScreenText("SHIFT+S to decrease radius of brush"),
        ScreenText("CTRL+E to iterate through edit modes"),
        emptyText(),
        ScreenText("F5 to save current map (TODO)", color = Color.LIGHT_GRAY),
        ScreenText("F9 to load a map (TODO)", color = Color.LIGHT_GRAY),
        position = BOTTOM
      )
    }
  }
}
