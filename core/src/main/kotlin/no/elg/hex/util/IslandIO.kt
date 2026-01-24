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
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.model.IslandDto
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.SplashIslandScreen

fun getIslandFileName(id: Int, preview: Boolean = false): String {
  val dir = if (preview) ISLAND_PREVIEWS_DIR else ISLAND_SAVES_DIR
  val ending = if (preview) "png" else ISLAND_FILE_ENDING
  return "$dir/island-$id.$ending"
}

fun getIslandFile(id: Int, preview: Boolean = false, allowInternal: Boolean = true): FileHandle {
  val path = getIslandFileName(id, preview)
  return getIslandFile(path, allowInternal)
}

fun getIslandFile(path: String, allowInternal: Boolean): FileHandle {
  val local = Gdx.files.local(path)
  return if (local.exists()) {
    local
  } else if (allowInternal) {
    Gdx.files.internal(path)
  } else {
    local
  }
}

fun play(metadata: FastIslandMetadata, island: Island? = null): Boolean {
  if (SplashIslandScreen.loading) {
    publishWarning("Already loading an island!")
    return false
  }
  return SplashIslandScreen(metadata, island).loadable
}

fun saveInitialIsland(metadata: FastIslandMetadata, island: Island): Boolean {
  island.ensureCapitalStartFunds()
  island.ensureTreesGrownOnTeamTurn()
  if (!island.validate()) {
    if (metadata.forTesting) {
      publishWarning("Island failed validation, but allowing save since forTesting is true")
    } else {
      publishError("Island failed validation")
      return false
    }
  }

  val file = getIslandFile(metadata.id, allowInternal = false)

  val existed = file.exists()
  if (file.isDirectory) {
    publishError("Failed to save island the name '${file.name()}' as there exists a directory with that name")
    return false
  }
  return try {
    file.writeString(island.createDto().serialize(), false)
    publishMessage("Successfully saved island '${file.name()}'", color = Color.GREEN)
    Hex.assets.islandPreviews.updatePreviewFromIsland(metadata, island) {
      if (!existed) {
        Hex.assets.islandFiles.fullFilesSearch()
      }
      Hex.assets.islandPreviews.sortIslands()
    }
    true
  } catch (e: Throwable) {
    publishError("Failed to saved island '${file.name()}'")
    e.printStackTrace()
    false
  }
}

fun loadInitialIsland(id: Int): Island = Island.deserialize(getIslandFile(id))

fun loadIslandSync(id: Int): Island {
  try {
    val progress = getIslandProgress(id)
    Gdx.app.trace("IS SPLASH") { "progress: $progress" }
    return if (!Hex.mapEditor && !progress.isNullOrBlank()) {
      Gdx.app.debug("IS SPLASH") { "Found progress for island $id" }
      Island.deserialize(progress)
    } else {
      Gdx.app.debug("IS SPLASH") { "No progress found for island $id" }
      loadInitialIsland(id)
    }
  } catch (e: Exception) {
    Gdx.app.postRunnable {
      publishError(
        "Failed to load island $id due to a ${e::class.simpleName}: ${e.message}",
        exception = e
      )
      Hex.screen = LevelSelectScreen()
    }
    throw IllegalStateException("Failed to load island $id", e)
  }
}

fun resetAllIslandProgress() {
  islandPreferences.clear()
  islandPreferences.flush()
  Hex.assets.islandPreviews.updateAllPreviewsFromIslandFiles()
  Hex.assets.islandPreviews.sortIslands()
}

fun IslandDto.serialize(): String = Hex.mapper.writeValueAsString(this)