package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.InputAdapter
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.LevelSelectScreen.mouseX
import no.elg.hex.screens.LevelSelectScreen.mouseY
import no.elg.hex.screens.LevelSelectScreen.previews
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4

/**
 * @author Elg
 */
object LevelSelectInputProcessor : InputAdapter() {

  private const val SCROLL_SPEED = 40f

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    when (button) {
      Buttons.LEFT -> {
        for (i in 0 until previews) {
          val (x, y, width, height) = LevelSelectScreen.rect(i)
          if (mouseX in x..x + width && mouseY in y..y + height) {
            LevelSelectScreen.play(i)
          }
        }
      }
      else -> return false
    }
    return true
  }

  override fun scrolled(amount: Int): Boolean {
    val (_, y, _, height) = LevelSelectScreen.rect(previews)
    val screenHeight = Gdx.graphics.height.toFloat()
    val oldY = LevelSelectScreen.camera.position.y
    LevelSelectScreen.camera.position.y = (oldY + amount * SCROLL_SPEED).coerceIn(screenHeight / 2..y + height - screenHeight)

    println("LevelSelectScreen.camera.position.y = ${LevelSelectScreen.camera.position.y}")

    return true
  }
}
