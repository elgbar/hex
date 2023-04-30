package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.screens.LevelCreationScreen
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.LevelSelectScreen.PREVIEWS_PER_ROW
import no.elg.hex.screens.LevelSelectScreen.camera
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.play
import no.elg.hex.util.trace
import java.lang.Float.max

/** @author Elg */
class LevelSelectInputProcessor : AbstractInput(true) {

  var lastY = camera.position.y
    private set

  private fun getHoveringIslandIndex(): Int {
    for (i in 0..PREVIEWS_PER_ROW) {
      val (x, y, width, height) = LevelSelectScreen.rect(i)
      if (mouseX in x..x + width && mouseY in y..y + height) {
        return i - PREVIEWS_PER_ROW
      }
    }

    for ((index, i) in Hex.assets.islandFiles.islandIds.withIndex()) {
      val (x, y, width, height) = LevelSelectScreen.rect(index + PREVIEWS_PER_ROW)
      if (mouseX in x..x + width && mouseY in y..y + height) {
        return i
      }
    }
    return INVALID_ISLAND_INDEX
  }

  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
    val index = getHoveringIslandIndex()
    Gdx.app.debug("SELECT", "Clicked on index $index")
    when {
      index == -PREVIEWS_PER_ROW -> Hex.screen = Hex.settingsScreen
      index == -PREVIEWS_PER_ROW + 1 -> {
        if (Hex.args.mapEditor) {
          Hex.screen = LevelCreationScreen()
        } else {
          return false
        }
      }

      index == -1 -> Hex.screen = Hex.tutorialScreen
      index in -PREVIEWS_PER_ROW..-1 -> return false
      index != INVALID_ISLAND_INDEX -> play(index)
      else -> return false
    }
    Hex.assets.clickSound?.play(Settings.volume)
    return true
  }

  private fun scroll(delta: Float) {
    val (_, y, _, height) = LevelSelectScreen.rect(Hex.assets.islandFiles.islandIds.size + PREVIEWS_PER_ROW * 2)
    val screenHeight = Gdx.graphics.height.toFloat()
    val minimum = screenHeight / 2f
    val maximum = max(minimum, y + height - screenHeight / 2f + LevelSelectScreen.padding)

    val newY = (lastY + delta).coerceIn(minimum..maximum)
    Gdx.app.trace("LSIP scroll", "lastY $lastY camera.position.y ${camera.position.y} newY $newY")
    camera.position.y = newY
    lastY = newY
    LevelSelectScreen.updateCamera()
  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    scroll(-Gdx.input.getDeltaY(0).toFloat())
    return true
  }

  override fun scrolled(amountX: Float, amountY: Float): Boolean {
    scroll(amountY * SCROLL_SPEED)
    return true
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Keys.FORWARD_DEL, Keys.DEL -> {
        if (Hex.args.mapEditor) {
          val index = getHoveringIslandIndex()
          if (index == INVALID_ISLAND_INDEX) return false
          Gdx.app.debug("SELECT", "Deleting island $index")
          val file = getIslandFile(index, preview = false, allowInternal = false)
          val filePreview = getIslandFile(index, preview = true, allowInternal = false)

          if (!file.delete()) {
            publishWarning("Failed to delete island $index")
          } else {
            val previewDel = filePreview.delete()
            // wait for file to synced with disk to make sure it appears as deleted when running file search
            while (file.exists()) {
              Thread.yield()
            }

            if (previewDel) {
              while (filePreview.exists()) {
                Thread.yield()
              }
            }
            Hex.assets.unload(getIslandFileName(index))
            Hex.assets.islandFiles.fullFilesSearch()
            LevelSelectScreen.dispose()
            publishMessage("Deleted island $index", color = Color.GREEN)
          }
          Hex.screen = LevelSelectScreen
        }
      }

      Keys.BACK -> Hex.platform.pause()

      else -> return false
    }
    return true
  }

  override fun show() {
    super.show()
    restoreScrollPosition()
  }

  fun restoreScrollPosition() {
    scroll(0f)
  }

  companion object {
    private const val SCROLL_SPEED = 50f
    private const val INVALID_ISLAND_INDEX = Int.MIN_VALUE
  }
}