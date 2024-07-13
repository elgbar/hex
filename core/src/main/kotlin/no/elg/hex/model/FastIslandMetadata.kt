package no.elg.hex.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import ktx.assets.disposeSafely
import no.elg.hex.Assets.Companion.ISLAND_METADATA_DIR
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.islandPreferences
import no.elg.hex.util.textureFromBytes
import java.util.Comparator.comparingInt
import java.util.function.ToIntFunction
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FastIslandMetadata(
  val id: Int,
  previewPixmap: ByteArray? = null,
  /**
   * The number of rounds the author has beaten the island in
   */
  var authorRoundsToBeat: Int = Island.UNKNOWN_ROUNDS_TO_BEAT,

  var modifier: PreviewModifier = PreviewModifier.NOTHING
) : Comparable<FastIslandMetadata>, Disposable {

  var previewPixmap: ByteArray? = previewPixmap
    set(value) {
      internalPreview.disposeSafely()
      internalPreview = null
      field = value
    }

  private var internalPreview: Texture? = null

  @get:JsonIgnore
  val preview: Texture?
    get() {
      if (internalPreview != null) return internalPreview
      internalPreview = textureFromBytes(previewPixmap ?: return null)
      return internalPreview
    }

  override fun compareTo(other: FastIslandMetadata): Int =
    comparingInt(
      ToIntFunction<FastIslandMetadata> { metadata ->
        if (Island.UNKNOWN_ROUNDS_TO_BEAT == metadata.authorRoundsToBeat) {
          Int.MAX_VALUE / 2
        } else {
          metadata.authorRoundsToBeat
        }
      }
    )
      .thenComparing(comparingInt(FastIslandMetadata::id).reversed())
      .compare(this, other)

  /**
   * Must be called on main thread
   */
  @OptIn(ExperimentalEncodingApi::class)
  fun save() {
    require(id >= 0) { "Island id must be positive, is $id" }
    requireNotNull(previewPixmap) { "A preview have not been generated" }
    requireNotNull(preview) { "Cannot save a preview that is not loadable, size of byte array is ${previewPixmap?.size}" }
    if (Hex.args.mapEditor) {
      require(modifier == PreviewModifier.NOTHING) { "Cannot save a modified preview in the map editor, is $modifier" }

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
    internalPreview.disposeSafely()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FastIslandMetadata

    if (id != other.id) return false
    if (authorRoundsToBeat != other.authorRoundsToBeat) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id
    result = 31 * result + authorRoundsToBeat
    return result
  }

  companion object {

    private fun getMetadataFileName(id: Int) = "island-metadata-$id.smile"

    fun getFileHandle(id: Int, isForWriting: Boolean) =
      getIslandFile("$ISLAND_METADATA_DIR/${getMetadataFileName(id)}", !isForWriting)

    fun loadInitial(id: Int): FastIslandMetadata? {
      return try {
        val serialized = getFileHandle(id, false).readBytes() ?: return null
        Hex.smileMapper.readValue<FastIslandMetadata>(serialized)
      } catch (e: Exception) {
        Gdx.app.error("IslandMetadataDto", "Failed to load initial island metadata for island $id", e)
        null
      }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun loadProgress(id: Int): FastIslandMetadata? {
      val raw = islandPreferences.getString(getMetadataFileName(id), null) ?: return null
      return try {
        val serialized = Base64.decode(raw)
        Hex.smileMapper.readValue<FastIslandMetadata>(serialized)
      } catch (e: Exception) {
        Gdx.app.error("IslandMetadataDto", "Failed to load island metadata progress for island $id", e)
        null
      }
    }

    fun load(id: Int): FastIslandMetadata {
      val savedMetadata = if (Hex.args.mapEditor || !islandPreferences.contains(getMetadataFileName(id))) {
        loadInitial(id)
      } else {
        loadProgress(id)
      }
      return savedMetadata ?: FastIslandMetadata(id)
    }
  }
}