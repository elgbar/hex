package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.island.IslandFiles
import no.elg.hex.screens.LevelCreationScreen
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.LevelSelectScreen.PREVIEWS_PER_ROW
import no.elg.hex.screens.LevelSelectScreen.camera
import no.elg.hex.screens.LevelSelectScreen.mouseX
import no.elg.hex.screens.LevelSelectScreen.mouseY
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.play
import java.lang.Float.max

/** @author Elg */
object LevelSelectInputProcessor : AbstractInput(true) {

  private const val SCROLL_SPEED = 1f
  private const val INVALID_ISLAND_INDEX = Int.MIN_VALUE

  private fun getHoveringIslandIndex(): Int {
    for ((index, i) in IslandFiles.islandIds.withIndex()) {
      val (x, y, width, height) = LevelSelectScreen.rect(index)
      if (mouseX in x..x + width && mouseY in y..y + height) {
        return i - PREVIEWS_PER_ROW
      }
    }
    return INVALID_ISLAND_INDEX
  }

  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
    LevelSelectScreen.projectCoordinates(x, y)
    val index = getHoveringIslandIndex()
    Gdx.app.debug("SELECT", "Clicked on index $index")
    when {
      index == -PREVIEWS_PER_ROW -> Hex.screen = Hex.settingsScreen
      index == -1 -> Hex.screen = Hex.tutorialScreen
      index != INVALID_ISLAND_INDEX -> play(index)
      Hex.args.mapEditor -> {
        val (ix, iy, width, height) = LevelSelectScreen.rect(IslandFiles.islandIds.size)
        if (mouseX in ix..ix + width && mouseY in iy..iy + height) {
          Hex.screen = LevelCreationScreen
        }
      }
      else -> return false
    }
    return true
  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    val delta = -Gdx.input.getDeltaY(0).toFloat()
    val (_, y, _, height) = LevelSelectScreen.rect(IslandFiles.islandIds.size)
    val screenHeight = Gdx.graphics.height.toFloat()
    val oldY = camera.position.y
    val min = screenHeight / 2f

    camera.position.y = (oldY + delta * SCROLL_SPEED)
      .coerceIn(min..max(min, y + height - screenHeight / 2f + LevelSelectScreen.padding))
    LevelSelectScreen.updateCamera()
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
            IslandFiles.fullFilesSearch()
            LevelSelectScreen.renderPreviews()
            publishMessage("Deleted island $index", color = Color.GREEN)
          }
          Hex.screen = LevelSelectScreen
        }
      }
      else -> return false
    }
    return true
  }
}
