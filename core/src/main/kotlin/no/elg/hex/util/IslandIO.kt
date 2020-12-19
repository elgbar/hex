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
import no.elg.hex.hud.ScreenText
import no.elg.hex.island.Island
import no.elg.hex.island.IslandFiles
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.screens.PlayableIslandScreen

fun getIslandFileName(slot: Int, preview: Boolean = false): String {
  return "${if (preview) ISLAND_PREVIEWS_DIR else ISLAND_SAVES_DIR}/island-$slot.${if (preview) "png" else ISLAND_FILE_ENDING}"
}

fun getIslandFile(slot: Int, preview: Boolean = false): FileHandle {
  val path = getIslandFileName(slot, preview)
  val local = Gdx.files.local(path)
  return if (local.exists()) local else Gdx.files.internal(path)
}

fun play(id: Int): Boolean {
  val assetId = getIslandFileName(id)
  val island: Island = if (Hex.assets.isLoaded(assetId)) {
    Hex.assets[assetId] ?: return false
  } else {
    val islandFile = getIslandFile(id)
    if (!islandFile.exists()) {
      publishWarning("Tried to play island $id, but no such island is loaded")
      return false
    }
    Hex.assets.load(assetId, Island::class.java)
    Hex.assets.finishLoadingAsset(assetId)
  }
  play(id, island)
  return true
}

fun play(id: Int, island: Island) {
  Gdx.app.postRunnable {
    Hex.screen =
      if (Hex.args.mapEditor) MapEditorScreen(id, island) else PlayableIslandScreen(id, island)
  }
}

fun saveIsland(id: Int, island: Island): Boolean {
  val file = Gdx.files.local(getIslandFileName(id))

  if (!island.validate()) {
    publishError("Island failed validation")
    return false
  }

  val existed = file.exists()
  if (file.isDirectory) {
    publishError("Failed to save island the name '${file.name()}' as the resulting file will be a directory.")
    return false
  }
  return try {
    file.writeString(island.serialize(), false)
    publishMessage(ScreenText("Successfully saved island '${file.name()}'", color = Color.GREEN))
    if (!existed) {
      IslandFiles.fullFilesSearch()
    }
    true
  } catch (e: Throwable) {
    publishMessage(ScreenText("Failed to saved island '${file.name()}'", color = Color.RED))
    e.printStackTrace()
    false
  }
}

fun Island.serialize(): String = Hex.mapper.writeValueAsString(this)
