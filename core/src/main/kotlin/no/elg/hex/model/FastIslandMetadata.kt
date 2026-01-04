package no.elg.hex.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.module.kotlin.readValue
import ktx.assets.disposeSafely
import ktx.async.skipFrame
import no.elg.hex.Assets.Companion.ISLAND_METADATA_DIR
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.SPECIAL_MAP
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.islandPreferences
import no.elg.hex.util.textureFromBytes
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@JsonPropertyOrder("id", "modifier", "forTesting", "authorRoundsToBeat", "userRoundsToBeat", "previewPixmap")
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

  fun isSpecialMap(): Boolean = authorRoundsToBeat == SPECIAL_MAP
  fun playerPlayed(): Boolean = !isSpecialMap() && userRoundsToBeat != Island.NEVER_PLAYED
  fun isUserBetterThanAuthor(): Boolean = playerPlayed() && userRoundsToBeat < authorRoundsToBeat
  fun wasNeverBeatenByAuthorButBeatenByUser(): Boolean = playerPlayed() && authorRoundsToBeat == Island.NEVER_PLAYED

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
      try {
        internalPreviewTexture = textureFromBytes(previewPixmap ?: return null)
      } catch (e: GdxRuntimeException) {
        Gdx.app.error("FastIslandMetadata", "Failed to load preview of $id!", e)
      }
      return internalPreviewTexture
    }

  /**
   * Loads the preview and skips a from if the texture was loaded. This is to allow the splash screen to update
   */
  suspend fun loadPreview(): Texture? {
    if (Settings.smoothScrolling) {
      val skipFrame = internalPreviewTexture == null
      val preview = this.preview
      if (skipFrame) {
        skipFrame()
      }
      return preview
    }
    return null
  }

  override fun compareTo(other: FastIslandMetadata): Int = Settings.levelSorter.comparator().compare(this, other)

  /**
   * Must be called on main thread
   */
  @OptIn(ExperimentalEncodingApi::class)
  fun save() {
    require(id >= 0) { "Island id must be positive, is $id" }
    requireNotNull(previewPixmap) { "A preview have not been generated" }
    // ok to write to file when Hex.args.writeARtBImprovements is true as the app will exit afterwards
    if (Hex.mapEditor || Hex.args.writeARtBImprovements) {
      // preview check only happens when we save it. For local saving we dont really care (user might have ran out of memory)
      requireNotNull(preview) { "Cannot save a preview that is not loadable, size of byte array is ${previewPixmap?.size}" }
      require(modifier == PreviewModifier.NOTHING) { "Cannot save a modified preview in the map editor, is $modifier" }
      require(userRoundsToBeat == Island.NEVER_PLAYED) { "userRoundsToBeat must be ${Island.NEVER_PLAYED} when saving initial island, is $userRoundsToBeat" }

      val fileHandle = getFileHandle(id, isForWriting = true)
      fileHandle.parent().mkdirs()
      val file = fileHandle.file()
      Hex.smileMapper.writeValue(file, this)
    } else {
      islandPreferences.putString(getMetadataFileName(id), Base64.encode(Hex.smileMapper.writeValueAsBytes(this)))
      islandPreferences.flush()
    }
  }

  override fun dispose() {
    clearPreviewTexture()
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

    private fun readIslandFromBytes(rawBytes: ByteArray, decode: Boolean): FastIslandMetadata {
      val serialized = if (decode) Base64.decode(rawBytes) else rawBytes
      return Hex.smileMapper.readValue<FastIslandMetadata>(serialized)
    }

    fun loadInitial(id: Int): FastIslandMetadata? =
      initialIslandMetadata.computeIfAbsent(id) {
        try {
          val fileHandle = getFileHandle(id, false)
          val rawBytes = fileHandle.readBytes()
          readIslandFromBytes(rawBytes, decode = false)
        } catch (e: Exception) {
          Gdx.app.error("IslandMetadataDto", "Failed to load initial island metadata for island $id", e)
          null
        }
      }

    fun loadProgress(id: Int): FastIslandMetadata? {
      val rawString = islandPreferences.getString(getMetadataFileName(id), null) ?: return null
      val rawBytes = rawString.toByteArray(US_ASCII)
      return try {
        readIslandFromBytes(rawBytes, decode = true)
      } catch (e: Exception) {
        Gdx.app.error("IslandMetadataDto", "Failed to load island metadata progress for island $id", e)
        null
      }?.also {
        // Clear up some memory by unloading initial metadata as we now have progress loaded
        clearInitialIslandMetadataCache(id)
      }
    }

    fun loadOrNull(id: Int): FastIslandMetadata? =
      if (Hex.mapEditor || getMetadataFileName(id) !in islandPreferences || getMetadataFileName(id) !in islandPreferences) {
        loadInitial(id)
      } else {
        loadProgress(id)
      }

    fun load(id: Int): FastIslandMetadata = loadOrNull(id) ?: FastIslandMetadata(id)
  }
}