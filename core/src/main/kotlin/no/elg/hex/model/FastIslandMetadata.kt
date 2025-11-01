package no.elg.hex.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.readValue
import ktx.assets.disposeSafely
import no.elg.hex.Assets.Companion.ISLAND_METADATA_DIR
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.island.Island
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.islandPreferences
import no.elg.hex.util.textureFromBytes
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FastIslandMetadata(
  val id: Int,
  previewPixmap: ByteArray? = null,
  /**
   * The number of rounds the author has beaten the island in
   */
  var authorRoundsToBeat: Int = Island.NEVER_PLAYED,

  var modifier: PreviewModifier = PreviewModifier.NOTHING,
  /**
   * If this island is for debugging and testing purposes only
   */
  var forTesting: Boolean = false,
  var userRoundsToBeat: Int = Island.NEVER_PLAYED
) : Comparable<FastIslandMetadata>,
  Disposable {

  fun isUserBetterThanAuthor(): Boolean = userRoundsToBeat != Island.NEVER_PLAYED && userRoundsToBeat < authorRoundsToBeat
  fun wasNeverBeatenByAuthorButBeatenByUser(): Boolean = authorRoundsToBeat == Island.NEVER_PLAYED && userRoundsToBeat != Island.NEVER_PLAYED

  var previewPixmap: ByteArray? = previewPixmap
    set(value) {
      clearPreviewTexture()
      field = value
    }

  private var internalPreviewTexture: Texture? = null

  fun clearPreviewTexture() {
    internalPreviewTexture.disposeSafely()
    internalPreviewTexture = null
  }

  @get:JsonIgnore
  val preview: Texture?
    get() {
      if (internalPreviewTexture != null) return internalPreviewTexture
      internalPreviewTexture = textureFromBytes(previewPixmap ?: return null)
      return internalPreviewTexture
    }

  override fun compareTo(other: FastIslandMetadata): Int = Settings.levelSorter.comparator().compare(this, other)

  /**
   * Must be called on main thread
   */
  @OptIn(ExperimentalEncodingApi::class)
  fun save() {
    require(id >= 0) { "Island id must be positive, is $id" }
    requireNotNull(previewPixmap) { "A preview have not been generated" }
    requireNotNull(preview) { "Cannot save a preview that is not loadable, size of byte array is ${previewPixmap?.size}" }
    // ok to write to file when Hex.args.writeARtBImprovements is true as the app will exit afterwards
    if (Hex.mapEditor || Hex.args.writeARtBImprovements) {
      require(modifier == PreviewModifier.NOTHING) { "Cannot save a modified preview in the map editor, is $modifier" }
      require(userRoundsToBeat == Island.NEVER_PLAYED) { "userRoundsToBeat must be ${Island.NEVER_PLAYED} when saving initial island, is $userRoundsToBeat" }

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
    internalPreviewTexture.disposeSafely()
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

    fun getFileHandle(id: Int, isForWriting: Boolean) = getIslandFile("$ISLAND_METADATA_DIR/${getMetadataFileName(id)}", !isForWriting)

    private val initialIslandMetadata: ConcurrentMap<Int, FastIslandMetadata> = ConcurrentHashMap()

    fun clearInitialIslandMetadataCache(id: Int) = initialIslandMetadata.remove(id)

    fun loadInitial(id: Int): FastIslandMetadata? =
      initialIslandMetadata.computeIfAbsent(id) {
        try {
          val serialized = getFileHandle(id, false).readBytes() ?: return@computeIfAbsent null
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
      }?.also {
        // Clear up some memory by unloading
        initialIslandMetadata.remove(id)
      }
    }

    fun loadOrNull(id: Int): FastIslandMetadata? =
      if (Hex.mapEditor || getMetadataFileName(id) !in islandPreferences) {
        loadInitial(id)
      } else {
        loadProgress(id)
      }

    fun load(id: Int): FastIslandMetadata = loadOrNull(id) ?: FastIslandMetadata(id)
  }
}