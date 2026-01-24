package no.elg.hex.preview

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import ktx.assets.load
import ktx.async.KtxAsync
import ktx.async.MainDispatcher
import ktx.async.skipFrame
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.model.FastIslandMetadata.Companion.loadInitial
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.fetch
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.isLoaded
import no.elg.hex.util.reportTiming
import no.elg.hex.util.safeUse
import no.elg.hex.util.toBytes
import no.elg.hex.util.trace
import no.elg.hex.util.useDispose
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class IslandPreviewCollection : Disposable {

  private val fastIslandPreviews = CopyOnWriteArrayList<FastIslandMetadata>()
  private var dirty = true

  val size get() = fastIslandPreviews.size
  var previewRendered = 0
    private set

  val ready: Boolean get() = _ready.get()
  private var _ready: AtomicBoolean = AtomicBoolean(false)

//  private val previewToRender = ConcurrentHashMap<Int, () -> FastIslandMetadata>()

  fun islandWithIndex(): Iterable<IndexedValue<FastIslandMetadata>> = sortedIslands().withIndex()

  fun sortedIslands(): Iterable<FastIslandMetadata> {
    if (dirty) {
      sortIslands()
    }
    return fastIslandPreviews
  }

  fun sortIslands() {
    dirty = false
    fastIslandPreviews.sort()
  }

  fun createPreviewFromIsland(island: Island, previewWidth: Int, previewHeight: Int, metadata: FastIslandMetadata): FrameBuffer {
    val islandScreen = PreviewIslandScreen(FastIslandMetadata(-1), island, true)
    islandScreen.resize(previewWidth, previewHeight)

    val buffer = FrameBuffer(
      Pixmap.Format.RGBA8888,
      previewWidth.coerceAtLeast(1),
      previewHeight.coerceAtLeast(1),
      false
    )
    buffer.safeUse {
      Hex.setClearColorAlpha(0f)
      Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or Hex.AA_BUFFER_CLEAR.value)
      islandScreen.render(0f)
      val camera = islandScreen.camera
      camera.zoom = .25f
      islandScreen.updateCamera()

      val width = camera.zoom * previewWidth / 2f
      val height = camera.zoom * previewHeight / 2f
      fun drawAsset(textureRegion: TextureAtlas.AtlasRegion) {
        islandScreen.batch.draw(
          textureRegion,
          camera.position.x - width / 2f,
          camera.position.y - height / 2f,
          width,
          height
        )
      }

      fun printText(text: String, heightMod: Float) {
        val font = Hex.assets.regularFontNotScaled
        font.color = Color.WHITE
        font.draw(
          islandScreen.batch,
          text,
          camera.position.x,
          camera.position.y + heightMod,
          0f,
          Align.center,
          false
        )
      }

      val belowAsset = height / 2f
      islandScreen.batch.safeUse(camera) {
        when (metadata.modifier) {
          PreviewModifier.SURRENDER -> {
            drawAsset(Hex.assets.surrender)
            printText("Surrendered on round ${island.round}", belowAsset)
          }

          PreviewModifier.LOST -> {
            drawAsset(Hex.assets.grave)
            printText("Lost on round ${island.round}", belowAsset)
          }

          PreviewModifier.AI_DONE -> {
            drawAsset(Hex.assets.castle)
            printText("Done on round ${island.round}", belowAsset)
          }

          PreviewModifier.WON -> {
            drawAsset(Hex.assets.capital)
            printText("Won on round ${island.round}", belowAsset)
          }

          PreviewModifier.NOTHING ->
            if (!Hex.mapEditor) {
              printText("Round ${island.round}", belowAsset)
            }
        }
      }
      Hex.setClearColorAlpha(1f)
    }
    islandScreen.dispose()
    return buffer
  }

  fun updateAllPreviewsFromIslandFiles() {
    val previewToRender = mutableMapOf<Int, FastIslandMetadata?>()
    for (islandId in Hex.assets.islandFiles.islandIds) {
      previewToRender[islandId] = FastIslandMetadata.loadOrNull(islandId, false)
    }
    updateAllPreviewsFromMetadata(previewToRender)
  }

  fun updateAllPreviewsFromMetadata(previewToRender: Map<Int, FastIslandMetadata?>) {
    _ready.set(false)
    if (Hex.assets.islandFiles.size == 0) {
      if (!Hex.args.`disable-island-loading`) {
        MessagesRenderer.publishError("Failed to find any islands to load")
        if (Hex.mapEditor) {
          MessagesRenderer.publishWarning("Do you have the correct island symbolic link in the project?")
          MessagesRenderer.publishWarning("There should be a error printed in the gradle logs")
          MessagesRenderer.publishWarning("To fix on windows you can enable Developer Mode")
        }
      }
      return
    }

    KtxAsync.launch(MainDispatcher) {
      disposePreviews()
      skipFrame()
      val localMetadata = mutableListOf<FastIslandMetadata>()
      for ((id, maybeMetadata) in previewToRender) {
        val metadata: FastIslandMetadata = if (Hex.args.`update-previews`) {
          val initialMetadata = loadInitial(id) ?: error("Require old metadata to re-render preview for island $id")
          updatePreviewFromIsland(initialMetadata)
          initialMetadata
        } else {
          maybeMetadata ?: continue
        }
        localMetadata += metadata
      }
      fastIslandPreviews.addAll(localMetadata)
      dirty = true

      if (!Hex.args.`update-previews`) {
        // When not updating previews, we can mark as ready here (before actually loading all previews)
        _ready.compareAndSet(false, true)
      }

      reportTiming("render all island previews", minSignificantTimeMs = 2000L) {
        previewRendered = 0
        // make a copy to avoid concurrent modification
        sortedIslands().forEach { metadata ->
          if (metadata.loadPreview() == null) {
            // abort to handle devices running out of memory
            // this operation only makes the main menu screen lag less, so we can skip it
            return@forEach
          }
          previewRendered++
          skipFrame()
        }
      }

      if (Hex.args.`update-previews`) {
        // If we're updating the previews it is nice to know how far we have come and when it is done
        _ready.compareAndSet(false, true)
      }
    }
  }

  fun updatePreviewFromIsland(metadata: FastIslandMetadata, maybeIsland: Island? = null, onDone: () -> Unit = {}): Job =
    KtxAsync.launch(Hex.asyncThread) {
      updatePreviewFromIslandSync(metadata, maybeIsland)
      onDone()
    }

  suspend fun updatePreviewFromIslandSync(metadata: FastIslandMetadata, maybeIsland: Island? = null) {
    val island = if (maybeIsland == null) {
      val islandFileName = getIslandFileName(metadata.id)
      if (!Hex.assets.isLoaded<Island>(islandFileName)) {
        Gdx.app.trace("Update preview") { "Island ${metadata.id} was not loaded, waiting for it to be loaded now..." }
        withContext(MainDispatcher) {
          Hex.assets.load<Island>(islandFileName)
          while (!Hex.assets.update()) {
            yield()
          }
        }
      }
      Hex.assets.fetch(islandFileName)
    } else {
      maybeIsland
    }

    withContext(MainDispatcher) {
      createPreviewFromIsland(island, PREVIEW_SIZE, PREVIEW_SIZE, metadata).useDispose { preview ->
        metadata.previewPixmap = preview.toBytes()
        metadata.save()
      }
      val existingIndex = fastIslandPreviews.indexOfFirst { it.id == metadata.id }
      if (existingIndex == NOT_IN_COLLECTION) {
        fastIslandPreviews.add(metadata)
      } else {
        fastIslandPreviews[existingIndex] = metadata
      }
      dirty = true
    }
  }

  fun removeIsland(id: Int) {
    val index = fastIslandPreviews.indexOfFirst { it.id == id }
    if (index != NOT_IN_COLLECTION) {
      val removeIndex = fastIslandPreviews.removeAt(index)
      removeIndex.dispose()
      dirty = true
    }
  }

  private fun disposePreviews() {
    val copy = fastIslandPreviews.toList()
    fastIslandPreviews.clear()
    copy.forEach(Disposable::dispose) // Make sure we dispose previews after they are removed from the collection
  }

  override fun dispose() {
    Gdx.app.trace("Island Previews") { "Disposing all previews" }
    disposePreviews()
  }

  companion object {
    const val PREVIEW_SIZE = 1024
    const val NOT_IN_COLLECTION = -1
  }
}