package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Assets.Companion.ISLAND_FILE_ENDING
import no.elg.hex.Assets.Companion.ISLAND_PREVIEWS_DIR
import no.elg.hex.Assets.Companion.ISLAND_SAVES_DIR
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishError
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.island.Island
import no.elg.hex.island.Island.IslandDto
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.screens.SplashIslandScreen

fun getIslandFileName(slot: Int, preview: Boolean = false): String {
  return "${if (preview) ISLAND_PREVIEWS_DIR else ISLAND_SAVES_DIR}/island-$slot.${if (preview) "png" else ISLAND_FILE_ENDING}"
}

fun getIslandFile(slot: Int, preview: Boolean = false, allowInternal: Boolean = true): FileHandle {
  val path = getIslandFileName(slot, preview)
  val local = Gdx.files.local(path)
  return if (local.exists()) local else if (allowInternal) Gdx.files.internal(path) else local
}

fun play(id: Int, island: Island? = null): Boolean {
  if (SplashIslandScreen.loading) {
    publishWarning("Already loading an island!")
    return false
  }
  return SplashIslandScreen(id, island).loadable
}

fun saveInitialIsland(id: Int, island: Island): Boolean {
  if (!island.validate()) {
    publishError("Island failed validation")
    return false
  }
  island.ensureCapitalStartFunds()

  val file = getIslandFile(id, allowInternal = false)

  val existed = file.exists()
  if (file.isDirectory) {
    publishError("Failed to save island the name '${file.name()}' as the resulting file will be a directory.")
    return false
  }
  return try {
    file.writeString(island.createDto().serialize(), false)
    publishMessage("Successfully saved island '${file.name()}'", color = Color.GREEN)
    if (!existed) {
      Hex.assets.islandFiles.fullFilesSearch()
    }
    Hex.assets.islandPreviews.updateSelectPreview(id, true)
    true
  } catch (e: Throwable) {
    publishError("Failed to saved island '${file.name()}'")
    e.printStackTrace()
    false
  }
}

fun resetAllIslandProgress() {
  PreviewIslandScreen.islandPreferences.clear()
  PreviewIslandScreen.islandPreferences.flush()
  Hex.assets.islandPreviews.renderPreviews()
}

fun IslandDto.serialize(): String = Hex.mapper.writeValueAsString(this)