package no.elg.hex.input

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.InputAdapter
import no.elg.hex.screens.IslandScreen

/**
 * @author Elg
 */
class GameInputProcessor(private val islandScreen: IslandScreen) : InputAdapter() {


  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    when (button) {
      Buttons.LEFT -> islandScreen.island.select(islandScreen.basicInputProcessor.cursorHex)
      else -> return false
    }
    return true
  }

}
