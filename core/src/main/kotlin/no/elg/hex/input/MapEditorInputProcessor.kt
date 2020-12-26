package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys.SHIFT_LEFT
import com.badlogic.gdx.Input.Keys.SHIFT_RIGHT
import com.badlogic.gdx.InputAdapter
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.util.calculateHexagonsWithinRadius

/** @author Elg */
class MapEditorInputProcessor(private val screen: MapEditorScreen) : InputAdapter() {

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (button == Buttons.LEFT) {
      val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false

      if (isShiftPressed()) {
        for (hexagon in screen.island.calculateHexagonsWithinRadius(cursorHex, screen.brushRadius)) {
          screen.editor.edit(hexagon)
        }
      } else {
        screen.editor.edit(cursorHex)
      }
      return true
    }
    return false
  }

  private fun isShiftPressed(): Boolean =
    Gdx.input.isKeyPressed(SHIFT_LEFT) || Gdx.input.isKeyPressed(SHIFT_RIGHT)
}
