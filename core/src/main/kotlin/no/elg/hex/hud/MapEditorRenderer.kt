package no.elg.hex.hud

import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.input.editor.PieceEditor
import no.elg.hex.input.editor.TeamEditor
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.screens.MapEditorScreen.Companion.MAX_BRUSH_SIZE
import no.elg.hex.screens.MapEditorScreen.Companion.MIN_BRUSH_SIZE
import no.elg.hex.util.toTitleCase

/** @author Elg */
class MapEditorRenderer(private val mapEditorScreen: MapEditorScreen) : FrameUpdatable {

  private val teamText by lazy {
    prefixText(
      "Selected team: ",
      callable = { mapEditorScreen.editor },
      format = { editor ->
        if (editor is TeamEditor.RandomizeTeam) {
          color = Color.PURPLE
          "Random"
        } else {
          color = Color.YELLOW
          mapEditorScreen.selectedTeam.name.lowercase().toTitleCase()
        }
      }
    )
  }

  private val pieceText by lazy {
    prefixText(
      "Selected piece: ",
      callable = { mapEditorScreen.editor },
      format = { editor ->
        if (editor is PieceEditor.RandomizePiece) {
          color = Color.PURPLE
          "Random"
        } else {
          color = Color.YELLOW
          mapEditorScreen.selectedPiece.simpleName?.toTitleCase() ?: "Unknown"
        }
      }
    )
  }

  private val brushText by lazy {
    variableText("Brush radius: ", callable = { mapEditorScreen.brushRadius }, MIN_BRUSH_SIZE, MAX_BRUSH_SIZE)
  }

  private val editorText by lazy {
    prefixText(
      "Editor: ",
      callable = { mapEditorScreen.editor },
      format = { editor ->
        if (editor.isNOP) {
          color = Color.RED
          "Disabled"
        } else {
          color = Color.GOLD
          editor.name
        }
      }
    )
  }

  private val lines = arrayOf(
    emptyText(),
    emptyText(),
    brushText,
    editorText,
    teamText,
    pieceText
  )

  override fun frameUpdate() {
    ScreenRenderer.drawAll(*lines, position = TOP_RIGHT)
  }
}