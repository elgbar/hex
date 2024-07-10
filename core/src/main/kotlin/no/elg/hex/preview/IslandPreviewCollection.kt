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
import ktx.assets.load
import ktx.async.KtxAsync
import ktx.collections.GdxArray
import ktx.graphics.center
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.screens.LevelSelectScreen.Companion.shownPreviewSize
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.fetch
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.isLoaded
import no.elg.hex.util.toBytes
import no.elg.hex.util.trace
import java.util.concurrent.atomic.AtomicInteger

class IslandPreviewCollection : Disposable {

  private val fastIslandPreviews = GdxArray<FastIslandMetadata>()
  private var dirty = true
  private val internalPreviewRendererQueue = GdxArray<Runnable>()
  private var lastPostedFrameId = 0L

  fun islandWithIndex(): Iterable<IndexedValue<FastIslandMetadata>> {
    synchronized(internalPreviewRendererQueue) {
      if (dirty) {
        sortIslands()
      }
      return fastIslandPreviews.withIndex()
    }
  }

  val size get() = fastIslandPreviews.size

  fun sortIslands() {
    dirty = false
    if (!Hex.args.mapEditor) {
      synchronized(internalPreviewRendererQueue) {
        fastIslandPreviews.sort()
      }
    }
  }

  private fun renderNextPreview() {
    synchronized(internalPreviewRendererQueue) {
      if (internalPreviewRendererQueue.isEmpty) {
        return
      }
      lastPostedFrameId = Gdx.graphics.frameId
      val runnable = internalPreviewRendererQueue.pop()
      Gdx.app.postRunnable {
        try {
          runnable.run()
        } finally {
          renderingCount.decrementAndGet()
          renderNextPreview()
        }
      }
    }
  }

  fun renderPreview(
    island: Island,
    previewWidth: Int,
    previewHeight: Int,
    metadata: FastIslandMetadata,
    onComplete: (preview: FrameBuffer) -> Unit
  ) {
    addPreviewRender(doRenderPreview(island, previewWidth, previewHeight, metadata, onComplete))
    renderNextPreview()
  }

  private fun addPreviewRender(runnable: Runnable) {
    renderingCount.incrementAndGet()
    synchronized(internalPreviewRendererQueue) {
      internalPreviewRendererQueue.add(runnable)
    }
  }

  private fun doRenderPreview(
    island: Island,
    previewWidth: Int,
    previewHeight: Int,
    metadata: FastIslandMetadata,
    onComplete: (preview: FrameBuffer) -> Unit
  ) = Runnable {
    val islandScreen = PreviewIslandScreen(FastIslandMetadata(-1), island, true)
    islandScreen.resize(previewWidth, previewHeight)
    islandScreen.centerCamera()
    val buffer = FrameBuffer(
      Pixmap.Format.RGBA8888,
      previewWidth.coerceAtLeast(1),
      previewHeight.coerceAtLeast(1),
      false
    )
    buffer.use {
      Hex.setClearColorAlpha(0f)
      Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or Hex.AA_BUFFER_CLEAR.value)
      islandScreen.updateCamera()
      islandScreen.render(0f)
      val camera = islandScreen.camera

      val widthOffset = camera.viewportWidth / 5
      val heightOffset = camera.viewportHeight / 5

      fun drawAsset(textureRegion: TextureAtlas.AtlasRegion) {
        islandScreen.batch.draw(
          textureRegion,
          widthOffset,
          heightOffset,
          camera.viewportWidth - widthOffset * 2,
          camera.viewportHeight - heightOffset * 2
        )
      }

      fun printText(text: String) {
        val font = Hex.assets.regularFont

        camera.setToOrtho(islandScreen.yDown, widthOffset, heightOffset)
        camera.center(widthOffset, heightOffset)

        islandScreen.batch.projectionMatrix = camera.combined

        font.color = Color.WHITE
        font.draw(
          islandScreen.batch,
          text,
          0f,
          (camera.viewportHeight - font.data.capHeight) / 2f,
          camera.viewportWidth,
          Align.center,
          false
        )
      }

      camera.setToOrtho(islandScreen.yDown, previewWidth.toFloat(), previewHeight.toFloat())
      islandScreen.batch.use(camera) {
        when (metadata.modifier) {
          PreviewModifier.SURRENDER -> drawAsset(Hex.assets.surrender)
          PreviewModifier.LOST -> drawAsset(Hex.assets.grave)
          PreviewModifier.AI_DONE -> drawAsset(Hex.assets.castle)
          PreviewModifier.WON -> {
            drawAsset(Hex.assets.capital)
            printText(island.round.toString())
          }

          PreviewModifier.NOTHING -> Unit
        }

        if (Hex.trace && !Hex.args.mapEditor) {
          printText("ARtB: ${metadata.authorRoundsToBeat}")
        }
      }
      camera.setToOrtho(islandScreen.yDown)
      Hex.setClearColorAlpha(1f)
    }
    islandScreen.dispose()
    onComplete(buffer)
  }

  fun renderPreviews() {
    if (Hex.assets.islandFiles.size == 0) {
      if (!Hex.args.`disable-island-loading`) {
        MessagesRenderer.publishError("Failed to find any islands to load")
        if (Hex.args.mapEditor) {
          MessagesRenderer.publishWarning("Do you have the correct island symbolic link in the project?")
          MessagesRenderer.publishWarning("There should be a error printed in the gradle logs")
          MessagesRenderer.publishWarning("To fix on windows you can enable Developer Mode")
        }
      }
      return
    }

    disposePreviews()

    for (id in Hex.assets.islandFiles.islandIds) {
      val metadata = FastIslandMetadata.load(id)
      if (Hex.args.`update-previews`) {
        updateSelectPreview(metadata)
        continue
      }
      synchronized(internalPreviewRendererQueue) {
        fastIslandPreviews.add(metadata)
        dirty = true
      }
    }
  }

  fun updateSelectPreview(
    metadata: FastIslandMetadata,
    maybeIsland: Island? = null
  ): Job =
    KtxAsync.launch(Hex.asyncThread) {
      val island = if (maybeIsland == null) {
        val islandFileName = getIslandFileName(metadata.id)
        if (!Hex.assets.isLoaded<Island>(islandFileName)) {
          Gdx.app.trace("Update preview") { "Island ${metadata.id} was not loaded, waiting for it to be loaded now..." }
          Hex.assets.load<Island>(islandFileName)
          while (!Hex.assets.update()) {
            Thread.yield()
          }
        }
        Hex.assets.fetch(islandFileName)
      } else {
        maybeIsland
      }

      renderPreview(island, PREVIEW_SIZE, PREVIEW_SIZE, metadata) { preview ->
        metadata.previewPixmap = preview.toBytes()
        metadata.save()
        synchronized(internalPreviewRendererQueue) {
          val existingIndex = fastIslandPreviews.indexOfFirst { it.id == metadata.id }
          if (existingIndex == -1) {
            fastIslandPreviews.add(metadata)
          } else {
            fastIslandPreviews.set(existingIndex, metadata)
          }
          dirty = true
        }
      }
    }

  private fun disposePreviews() {
    synchronized(internalPreviewRendererQueue) {
      fastIslandPreviews.map(Disposable::dispose)
      fastIslandPreviews.clear()
    }
  }

  override fun dispose() {
    Gdx.app.trace("Island Previews") { "Disposing all previews" }
    disposePreviews()
  }

  companion object {
    private const val PREVIEW_SIZE = 1024

    val renderingCount = AtomicInteger(0)
    val renderingPreviews: Boolean get() = renderingCount.get() > 0
  }
}