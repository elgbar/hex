package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.screens.LevelSelectScreen
import org.hexworks.mixite.core.api.HexagonalGridLayout.RECTANGULAR

/** @author Elg */
object LevelCreationInputProcessor : InputAdapter() {

  var width = 10
  var height = 10
  var layout = RECTANGULAR

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    Gdx.app.debug(
        "CREATOR",
        "Creating island ${LevelSelectScreen.islandAmount} with a dimension of $width x $height and layout $layout")
    LevelSelectScreen.play(LevelSelectScreen.islandAmount, Island(width, height, layout))
    return true
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Keys.ESCAPE -> Hex.screen = LevelSelectScreen
      else -> return false
    }
    return true
  }
}
