package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Assets.Companion.ISLAND_FILE_ENDING
import no.elg.hex.Assets.Companion.ISLAND_METADATA_DIR
import no.elg.hex.Assets.Companion.ISLAND_PREVIEWS_DIR
import no.elg.hex.Assets.Companion.ISLAND_SAVES_DIR
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishError
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.island.Island
import no.elg.hex.island.Island.IslandDto
import no.elg.hex.island.IslandFiles
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.screens.SplashIslandScreen


fun getIslandJsonFileName(slot: Int): String {
  return "$ISLAND_SAVES_DIR/island-$slot.$ISLAND_FILE_ENDING"
}

fun getIslandPreviewFileName(slot: Int): String {
  return "$ISLAND_PREVIEWS_DIR/island-$slot.png"
}

fun getIslandMetadataFileName(slot: Int): String {
  return "$ISLAND_METADATA_DIR/island-metadata-$slot.json"
}

enum class IslandFileType {
  JSON,
  PREVIEW,
  METADATA
}

fun getIslandFile(slot: Int, fileType: IslandFileType = IslandFileType.JSON, allowInternal: Boolean = true): FileHandle {
  val path = when (fileType) {
    IslandFileType.JSON -> getIslandJsonFileName(slot)
    IslandFileType.PREVIEW -> getIslandPreviewFileName(slot)
    IslandFileType.METADATA -> getIslandMetadataFileName(slot)
  }
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

fun saveIsland(id: Int, island: Island): Boolean {
  if (!island.validate()) {
    publishError("Island failed validation")
    return false
  }
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
      IslandFiles.fullFilesSearch()
    }
    LevelSelectScreen.updateSelectPreview(id, true)
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
  if (Hex.screen == LevelSelectScreen) {
    LevelSelectScreen.renderPreviews()
  }
}

fun IslandDto.serialize(): String = Hex.mapper.writeValueAsString(this)