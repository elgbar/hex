package no.elg.hex.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Assets.Companion.ISLAND_METADATA_DIR
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.util.decodeStringToTexture
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.isLazyInitialized
import no.elg.hex.util.islandPreferences
import no.elg.hex.util.loadIslandSync

data class FastIslandMetadata(
  val id: Int,
  val authorRoundsToBeat: Int = Island.UNKNOWN_ROUNDS_TO_BEAT,
  val previewPixmap: String
) : Comparable<FastIslandMetadata>, Disposable {

  @get:JsonIgnore
  val preview: Texture by lazy { decodeStringToTexture(previewPixmap) }

  @get:JsonIgnore
  val island: Island by lazy { loadIslandSync(id) }

  override fun compareTo(other: FastIslandMetadata): Int {
    return Comparator.comparingInt(FastIslandMetadata::authorRoundsToBeat)
      .thenComparingInt(FastIslandMetadata::id)
      .compare(this, other)
  }

  fun save() {
    if (Hex.args.mapEditor) {
      val fileHandle = getFileHandle(id, true)
      fileHandle.parent().mkdirs()
      val file = fileHandle.file()
      Hex.mapper.writeValue(file, this)
    } else {
      islandPreferences.putString(getName(id), Hex.mapper.writeValueAsString(this))
      islandPreferences.flush()
    }
  }

  override fun dispose() {
    if (::preview.isLazyInitialized) {
      preview.dispose()
    }
  }

  companion object {

    private fun getName(id: Int) = "island-metadata-$id.json"

    private fun getFileHandle(id: Int, isForWriting: Boolean) =
      getIslandFile("$ISLAND_METADATA_DIR/${getName(id)}", !isForWriting)

    private val charset = Charsets.UTF_8.toString()

    fun load(id: Int): FastIslandMetadata? {
      val name = "island-metadata-$id.json"
      val prefJson = islandPreferences.getString(name, null)
      return try {
        val json = if (Hex.args.mapEditor || prefJson.isNullOrBlank()) {
          getFileHandle(id, false).readString(charset)
        } else {
          prefJson
        }
        Hex.mapper.readValue(json)
      } catch (e: Exception) {
        Gdx.app.error("IslandMetadataDto", "Failed to find a metadata dto with id $id", e)
        null
      }
    }
  }
}