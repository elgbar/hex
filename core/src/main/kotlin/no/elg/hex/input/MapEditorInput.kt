package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Hex
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.input.MapEditorInput.EditMode.DELETE
import no.elg.hex.map.Map
import no.elg.hex.util.findHexagonsWithinRadius
import no.elg.hex.util.getData
import kotlin.math.max
import kotlin.math.min

/**
 * @author Elg
 */
object MapEditorInput : InputAdapter() {

  enum class EditMode(val newOpaqueness: Boolean) {
    ADD(false), DELETE(true)
  }

  private var savedMap: String = ""

  const val MAX_BRUSH_SIZE = 10
  const val MIN_BRUSH_SIZE = 1

  var brushRadius: Int = 1
    private set

  var editMode: EditMode = DELETE
    private set


  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (button == Buttons.LEFT) {
      val cursorHex = BasicInputHandler.cursorHex ?: return true
      if (isShiftPressed()) {
        for (hexagon in cursorHex.findHexagonsWithinRadius(brushRadius)) {
          hexagon.getData().isOpaque = editMode.newOpaqueness
        }
      } else {
        cursorHex.getData().isOpaque = editMode.newOpaqueness
      }
      return true
    }
    return false
  }

  override fun keyDown(keycode: Int): Boolean {
    if (keycode == Keys.F1) {
      MapEditorRenderer.showHelp = !MapEditorRenderer.showHelp
      return true
    } else if (keycode == Keys.W && isShiftPressed()) {
      brushRadius = min(brushRadius + 1, MAX_BRUSH_SIZE)
      return true
    } else if (keycode == Keys.S && isShiftPressed()) {
      brushRadius = max(brushRadius - 1, MIN_BRUSH_SIZE)
      return true
    } else if (keycode == Keys.E && isControlPressed()) {
      editMode = editMode.next(EditMode.values())
      return true
    } else if (keycode == Keys.F5) {

      savedMap = Hex.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Hex.map)
      println("a = ${savedMap}")

    } else if (keycode == Keys.F9) {
      val new = Hex.mapper.readValue<Map>(savedMap)

      require(new === Hex.map)
    }
    return false
  }

  private fun isShiftPressed(): Boolean = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT)
  private fun isControlPressed(): Boolean = Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT)

  private fun <E : Enum<E>> Enum<E>.next(values: Array<E>): E {
//  val values: Array<E> = E::class::enumValues
    return if (ordinal + 1 == values.size) return values[0] else values[ordinal + 1]
  }
}
