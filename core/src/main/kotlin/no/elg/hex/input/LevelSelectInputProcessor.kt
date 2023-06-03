package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.screens.LevelCreationScreen
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.LevelSelectScreen.Companion.NON_ISLAND_SCALE
import no.elg.hex.screens.LevelSelectScreen.Companion.PREVIEWS_PER_ROW
import no.elg.hex.screens.LevelSelectScreen.Companion.paddingX
import no.elg.hex.screens.LevelSelectScreen.Companion.paddingY
import no.elg.hex.screens.LevelSelectScreen.Companion.shownPreviewSize
import no.elg.hex.screens.SettingsScreen
import no.elg.hex.screens.TutorialScreen
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.play
import java.lang.Float.max

/** @author Elg */
class LevelSelectInputProcessor(private val screen: LevelSelectScreen) : AbstractInput(true) {

  /**
   * @param scale in range 0..1
   * @param horzOffset in range 0..1
   */
  fun slotRect(index: Int, scale: Float = 1f, horzOffset: Float = 0.5f): Rectangle {
    val gridX = index % PREVIEWS_PER_ROW
    val gridY = index / PREVIEWS_PER_ROW

    val size = shownPreviewSize
    val paddedSize = paddingX + size

    return Rectangle(
      paddingX + paddedSize * gridX + size * horzOffset * (1f - scale),
      paddingY + paddedSize * (gridY - (1f - NON_ISLAND_SCALE)) + size * (1f - scale),
      size * scale,
      size * scale
    )
  }

  private fun getHoveringIslandId(): Int {
    for (i in 0 until PREVIEWS_PER_ROW) {
      val (x, y, width, height) = slotRect(i)
      if (mouseX in x..x + width && mouseY in y..y + height) {
        return i - PREVIEWS_PER_ROW
      }
    }

    for ((index, metadata) in Hex.assets.islandPreviews.islandPreviews.withIndex()) {
      val (x, y, width, height) = slotRect(index + PREVIEWS_PER_ROW)
      if (mouseX in x..x + width && mouseY in y..y + height) {
        return metadata.id
      }
    }
    return INVALID_ISLAND_INDEX
  }

  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
    val id = getHoveringIslandId()
    Gdx.app.debug("SELECT", "Clicked on id $id")
    when {
      id == -PREVIEWS_PER_ROW -> Hex.screen = SettingsScreen()
      id == -PREVIEWS_PER_ROW + 1 -> {
        if (Hex.args.mapEditor) {
          Hex.screen = LevelCreationScreen()
        } else {
          return false
        }
      }

      id == -1 -> Hex.screen = TutorialScreen()
      id in -PREVIEWS_PER_ROW..-1 -> return false
      id != INVALID_ISLAND_INDEX -> play(id)
      else -> return false
    }
    Hex.assets.clickSound?.play(Settings.volume)
    return true
  }

  private fun scroll(delta: Float) {
    val (_, y, _, height) = slotRect(Hex.assets.islandFiles.size + PREVIEWS_PER_ROW * 2)
    val screenHeight = Gdx.graphics.height.toFloat()
    val minimum = screenHeight / 2f
    val maximum = max(minimum, y + height - screenHeight / 2f + paddingX)

    val newY = (lastY + delta).coerceIn(minimum..maximum)
    screen.camera.position.y = newY
    lastY = newY
    screen.updateCamera()
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
          val id = getHoveringIslandId()
          if (id == INVALID_ISLAND_INDEX) return false
          Gdx.app.debug("SELECT", "Deleting island $id")
          val file = getIslandFile(id, preview = false, allowInternal = false)
          val filePreview = getIslandFile(id, preview = true, allowInternal = false)

          if (!file.delete()) {
            publishWarning("Failed to delete island $id")
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
            Hex.assets.unload(getIslandFileName(id))
            Hex.assets.islandFiles.fullFilesSearch()
            screen.dispose()
            publishMessage("Deleted island $id", color = Color.GREEN)
          }
          Hex.screen = screen
        }
      }

      Keys.BACK -> Hex.platform.pause()

      else -> return false
    }
    return true
  }

  fun restoreScrollPosition() {
    scroll(0f)
  }

  companion object {
    private const val SCROLL_SPEED = 50f
    private const val INVALID_ISLAND_INDEX = Int.MIN_VALUE

    var lastY: Float = 0f
      private set
  }
}