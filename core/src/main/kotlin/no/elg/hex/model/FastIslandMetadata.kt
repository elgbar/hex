package no.elg.hex.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Assets.Companion.ISLAND_METADATA_DIR
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.isLazyInitialized
import no.elg.hex.util.islandPreferences
import no.elg.hex.util.loadIslandSync
import no.elg.hex.util.textureFromBytes
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class FastIslandMetadata(
  val id: Int,
  val authorRoundsToBeat: Int = Island.UNKNOWN_ROUNDS_TO_BEAT,
  val previewPixmap: ByteArray
) : Comparable<FastIslandMetadata>, Disposable {

  @get:JsonIgnore
  val preview: Texture by lazy { textureFromBytes(previewPixmap) }

  @get:JsonIgnore
  val island: Island by lazy { loadIslandSync(id) }

  override fun compareTo(other: FastIslandMetadata): Int {
    return Comparator.comparingInt(FastIslandMetadata::authorRoundsToBeat)
      .thenComparingInt(FastIslandMetadata::id)
      .compare(this, other)
  }

  @OptIn(ExperimentalEncodingApi::class)
  fun save() {
    if (Hex.args.mapEditor) {
      val fileHandle = getFileHandle(id, true)
      fileHandle.parent().mkdirs()
      val file = fileHandle.file()
      Hex.smileMapper.writeValue(file, this)
    } else {
      islandPreferences.putString(getMetadataFileName(id), Base64.encode(Hex.smileMapper.writeValueAsBytes(this)))
      islandPreferences.flush()
    }
  }

  override fun dispose() {
    if (::preview.isLazyInitialized) {
      preview.dispose()
    }
  }

  companion object {

    private fun getMetadataFileName(id: Int) = "island-metadata-$id.smile"

    private fun getFileHandle(id: Int, isForWriting: Boolean) =
      getIslandFile("$ISLAND_METADATA_DIR/${getMetadataFileName(id)}", !isForWriting)

    private val charset = Charsets.UTF_8.toString()

    @OptIn(ExperimentalEncodingApi::class)
    fun load(id: Int): FastIslandMetadata? {
      val pref: String? = islandPreferences.getString(getMetadataFileName(id), null)
      return try {
        val bytes = if (Hex.args.mapEditor || pref.isNullOrEmpty()) {
          getFileHandle(id, false).readBytes()
        } else {
          Base64.decode(pref)
        }
        Hex.smileMapper.readValue(bytes)
      } catch (e: Exception) {
        Gdx.app.error("IslandMetadataDto", "Failed to find a metadata dto with id $id", e)
        null
      }
    }
  }
}