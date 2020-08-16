package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Assets
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.hud.ScreenText
import no.elg.hex.island.Island
import no.elg.hex.screens.IslandScreen

fun getIslandFileName(slot: Int) =
    "${Assets.ISLAND_SAVES_DIR}/island-$slot.${Assets.ISLAND_FILE_ENDING}"

fun getIslandFile(slot: Int): FileHandle {
  val path = getIslandFileName(slot)
  val internal = Gdx.files.internal(path)
  return if (internal.exists()) internal else Gdx.files.local(path)
}

fun play(id: Int) {
  play(id, Hex.assets.get(getIslandFileName(id)))
}

fun play(id: Int, island: Island) {
  Gdx.app.postRunnable { Hex.screen = IslandScreen(id, island) }
}

fun saveIsland(id: Int, island: Island): Boolean {
  val file = Gdx.files.local(getIslandFileName(id))

  if (!island.validate()) {
    MessagesRenderer.publishMessage(ScreenText("Island failed validation", color = Color.RED))
    return false
  }

  if (file.isDirectory) {
    MessagesRenderer.publishMessage(
        ScreenText(
            "Failed to save island the name '${file.name()}' as the resulting file will be a directory.",
            color = Color.RED))
    return false
  }
  return try {
    file.writeString(island.serialize(), false)
    MessagesRenderer.publishMessage(
        ScreenText("Successfully saved island '${file.name()}'", color = Color.GREEN))
    true
  } catch (e: Throwable) {
    MessagesRenderer.publishMessage(
        ScreenText("Failed to saved island '${file.name()}'", color = Color.RED))
    e.printStackTrace()
    false
  }
}

fun Island.serialize(): String = Hex.mapper.writeValueAsString(this)