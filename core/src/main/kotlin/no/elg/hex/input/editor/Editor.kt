package no.elg.hex.input.editor

import com.badlogic.gdx.graphics.Color
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hud.ScreenText
import org.hexworks.mixite.core.api.Hexagon

/** @author Elg */
interface Editor {
  val name: String
    get() =
      requireNotNull(this::class.simpleName) {
        "Subclass of ${Editor::class::simpleName} cannot be anonymous"
      }

  val isNOP
    get() = false

  fun edit(hexagon: Hexagon<HexagonData>)

  companion object {
    fun editorText(
      editor: Editor,
      bold: Boolean = false,
      italic: Boolean = false,
      next: ScreenText? = null
    ): ScreenText {
      return if (editor.isNOP)
        ScreenText("Disabled", color = Color.RED, bold = bold, italic = italic, next = next)
      else ScreenText(editor.name, color = Color.GOLD, bold = bold, italic = italic, next = next)
    }
  }
}

object NOOPEditor : Editor {
  override val isNOP
    get() = true
  override fun edit(hexagon: Hexagon<HexagonData>) {
    /*NO OP*/
  }
}
