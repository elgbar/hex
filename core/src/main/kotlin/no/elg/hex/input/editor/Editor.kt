package no.elg.hex.input.editor

import com.badlogic.gdx.graphics.Color
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hud.ScreenText
import org.hexworks.mixite.core.api.Hexagon

/**
 * @author Elg
 */
abstract class Editor {
  val name: String = requireNotNull(this::class.simpleName) { "Subclass of ${TeamEditor::class::simpleName} cannot be anonymous" }

  open val isNOP = false

  open fun edit(hexagon: Hexagon<HexagonData>) {}

  companion object {
    fun editorText(editor: Editor,
                   bold: Boolean = false,
                   italic: Boolean = false,
                   next: ScreenText? = null): ScreenText {
      return if (editor.isNOP) ScreenText("Disabled", color = Color.RED, bold = bold, italic = italic, next = next)
      else ScreenText(editor.name, color = Color.GOLD, bold = bold, italic = italic, next = next)
    }
  }
}


