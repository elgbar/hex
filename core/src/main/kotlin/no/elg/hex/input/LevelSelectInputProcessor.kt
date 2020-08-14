package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import java.lang.Float.max
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.ScreenText
import no.elg.hex.screens.LevelCreationScreen
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.LevelSelectScreen.camera
import no.elg.hex.screens.LevelSelectScreen.islandAmount
import no.elg.hex.screens.LevelSelectScreen.mouseX
import no.elg.hex.screens.LevelSelectScreen.mouseY
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4

/** @author Elg */
object LevelSelectInputProcessor : InputAdapter() {

  private const val SCROLL_SPEED = 40f
  private const val INVALID_ISLAND_INDEX = -1

  private fun getHoveringIslandIndex(): Int {
    for (i in 0 until islandAmount) {
      val (x, y, width, height) = LevelSelectScreen.rect(i)
      if (mouseX in x..x + width && mouseY in y..y + height) {
        return i
      }
    }
    return INVALID_ISLAND_INDEX
  }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    when (button) {
      Buttons.LEFT -> {
        val index = getHoveringIslandIndex()
        if (index != INVALID_ISLAND_INDEX) {
          LevelSelectScreen.play(index)
        } else {
          val (x, y, width, height) = LevelSelectScreen.rect(islandAmount)
          if (mouseX in x..x + width && mouseY in y..y + height) {
            Hex.screen = LevelCreationScreen
          }
        }
      }
      else -> return false
    }
    return true
  }

  override fun scrolled(amount: Int): Boolean {
    val (_, y, _, height) = LevelSelectScreen.rect(islandAmount)
    val screenHeight = Gdx.graphics.height.toFloat()
    val oldY = camera.position.y
    val min = screenHeight / 2f

    camera.position.y =
        (oldY + amount * SCROLL_SPEED).coerceIn(min..max(min, y + height - screenHeight))
    LevelSelectScreen.updateCamera()
    return true
  }
}
