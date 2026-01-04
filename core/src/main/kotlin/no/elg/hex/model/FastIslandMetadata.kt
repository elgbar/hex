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
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.util.compressXZ
import no.elg.hex.util.decompressXZ
import no.elg.hex.util.encodeB85
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.islandPreferences
import no.elg.hex.util.textureFromBytes
import no.elg.hex.util.tryDecompressB85AndDecompressXZ
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
    val skipFrame = internalPreviewTexture == null
    val preview = this.preview
    if (skipFrame) {
      skipFrame()
    }
    return preview
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
    val serializedThis = Hex.mapper.writeValueAsBytes(this)
    val compressed = compressXZ(serializedThis) ?: serializedThis
    if (Hex.mapEditor || Hex.args.writeARtBImprovements) {
      // preview check only happens when we save it. For local saving we dont really care (user might have ran out of memory)
      requireNotNull(preview) { "Cannot save a preview that is not loadable, size of byte array is ${previewPixmap?.size}" }
      require(modifier == PreviewModifier.NOTHING) { "Cannot save a modified preview in the map editor, is $modifier" }
      require(userRoundsToBeat == Island.NEVER_PLAYED) { "userRoundsToBeat must be ${Island.NEVER_PLAYED} when saving initial island, is $userRoundsToBeat" }

      val fileHandle = getFileHandle(id, isForWriting = true, useNewEnding = true)
      fileHandle.parent().mkdirs()
      val file = fileHandle.file()
      file.writeBytes(compressed)
    } else {
      islandPreferences.putString(getMetadataFileName(id, useNewEnding = true), encodeB85(compressed))
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

    private fun getMetadataFileName(id: Int, useNewEnding: Boolean) = "island-metadata-$id." + if (useNewEnding) "json.xz" else "smile"

    fun getFileHandle(id: Int, isForWriting: Boolean, useNewEnding: Boolean) = getIslandFile("$ISLAND_METADATA_DIR/${getMetadataFileName(id, useNewEnding)}", !isForWriting)

    private val initialIslandMetadata: ConcurrentMap<Int, FastIslandMetadata> = ConcurrentHashMap()

    fun clearInitialIslandMetadataCache(id: Int) = initialIslandMetadata.remove(id)

    private fun readIslandFromBytes(rawBytes: ByteArray, useNewEnding: Boolean, decode: Boolean): FastIslandMetadata =
      if (useNewEnding) {
        val serialized = if (decode) tryDecompressB85AndDecompressXZ(rawBytes) else decompressXZ(rawBytes)
        Hex.mapper.readValue<FastIslandMetadata>(serialized ?: error("Failed to decode island"))
      } else {
        val serialized = if (decode) Base64.decode(rawBytes) else rawBytes
        @Suppress("DEPRECATION") // Legacy support for old format
        Hex.smileMapper.readValue<FastIslandMetadata>(serialized)
      }

    fun loadInitial(id: Int): FastIslandMetadata? =
      initialIslandMetadata.computeIfAbsent(id) {
        try {
          val fileHandle = getFileHandle(id, false, useNewEnding = true)
          val rawBytes = fileHandle.readBytes()
          readIslandFromBytes(rawBytes, true, decode = false)
        } catch (e: Exception) {
          Gdx.app.error("IslandMetadataDto", "Failed to load initial island metadata for island $id", e)
          null
        }
      }

    fun loadProgress(id: Int): FastIslandMetadata? {
      var useNewEnding = true
      val rawString = islandPreferences.getString(getMetadataFileName(id, useNewEnding = true), null) ?: let {
        useNewEnding = false
        islandPreferences.getString(getMetadataFileName(id, useNewEnding = false), null)
      } ?: return null
      val rawBytes = rawString.toByteArray(US_ASCII)
      return try {
        readIslandFromBytes(rawBytes, useNewEnding, decode = true)
      } catch (e: Exception) {
        Gdx.app.error("IslandMetadataDto", "Failed to load island metadata progress for island $id", e)
        null
      }?.also {
        // Clear up some memory by unloading initial metadata as we now have progress loaded
        clearInitialIslandMetadataCache(id)
      }
    }

    fun loadOrNull(id: Int): FastIslandMetadata? =
      if (Hex.mapEditor || getMetadataFileName(id, useNewEnding = true) !in islandPreferences || getMetadataFileName(id, useNewEnding = false) !in islandPreferences) {
        loadInitial(id)
      } else {
        loadProgress(id)
      }

    fun load(id: Int): FastIslandMetadata = loadOrNull(id) ?: FastIslandMetadata(id)
  }
}