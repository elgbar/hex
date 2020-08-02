package no.elg.hex.input

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.InputAdapter
import no.elg.hex.island.Island
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData

/**
 * @author Elg
 */
class GameInputProcessor(private val islandScreen: IslandScreen) : InputAdapter() {


  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    when (button) {
      Buttons.LEFT -> {
        val hex = islandScreen.basicInputProcessor.cursorHex ?: return false
        if (hex.getData(islandScreen.island).team != Island.PLAYER_TEAM) return false
        islandScreen.island.select(hex)
      }
      else -> return false
    }
    return true
  }

}
