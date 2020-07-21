package no.elg.hex.input

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.InputAdapter
import no.elg.hex.Hex

/**
 * @author Elg
 */
object GameInputHandler : InputAdapter() {


  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    when (button) {
      Buttons.LEFT -> Hex.island.select()
      else -> return false
    }
    return true
  }

}
