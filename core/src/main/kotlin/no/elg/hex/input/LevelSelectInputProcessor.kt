package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import java.lang.Float.max
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.hud.ScreenText
import no.elg.hex.screens.LevelCreationScreen
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.LevelSelectScreen.PREVIEW_PADDING_PERCENT
import no.elg.hex.screens.LevelSelectScreen.camera
import no.elg.hex.screens.LevelSelectScreen.islandAmount
import no.elg.hex.screens.LevelSelectScreen.mouseX
import no.elg.hex.screens.LevelSelectScreen.mouseY
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.play

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
          play(index)
        } else if (Hex.args.mapEditor) {
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
        (oldY + amount * SCROLL_SPEED).coerceIn(
            min..max(min, y + height - screenHeight / 2f + LevelSelectScreen.padding))
    LevelSelectScreen.updateCamera()
    return true
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Keys.FORWARD_DEL, Keys.DEL -> {
        val index = getHoveringIslandIndex()
        if (index == INVALID_ISLAND_INDEX) return false
        Gdx.app.debug("SELECT", "Deleting island $index")
        val fileName = getIslandFileName(index)

        if (!Gdx.files.local(fileName).delete()) {
          publishWarning("Failed to delete island $index")
        } else {
          Hex.assets.unload(fileName)
          publishMessage(ScreenText("Deleted island $index", Color.GREEN))
        }

        Hex.screen = LevelSelectScreen
      }
      else -> return false
    }
    return true
  }
}
