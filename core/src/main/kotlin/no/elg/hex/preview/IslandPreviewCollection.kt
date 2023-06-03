package no.elg.hex.preview

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import kotlinx.coroutines.launch
import ktx.assets.disposeSafely
import ktx.assets.load
import ktx.async.KtxAsync
import ktx.collections.GdxArray
import ktx.collections.sortBy
import ktx.graphics.center
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.model.IslandMetadata
import no.elg.hex.screens.LevelSelectScreen.Companion.shownPreviewSize
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.decodeStringToTexture
import no.elg.hex.util.fetch
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.getPrefName
import no.elg.hex.util.getProgress
import no.elg.hex.util.isLoaded
import no.elg.hex.util.islandPreferences
import no.elg.hex.util.loadIslandSync
import no.elg.hex.util.reportTiming
import no.elg.hex.util.saveScreenshotAsString
import no.elg.hex.util.takeScreenshot
import no.elg.hex.util.trace
import java.util.concurrent.atomic.AtomicInteger

class IslandPreviewCollection : Disposable {

  private val islandPreviews = GdxArray<IslandMetadata>()
  private val internalPreviewRendererQueue = GdxArray<Runnable>()
  private var lastPostedFrameId = 0L
  private val screen get() = Hex.screen

  fun islandWithIndex(): Iterable<IndexedValue<IslandMetadata>> {
    synchronized(internalPreviewRendererQueue) {
      return islandPreviews.toList().withIndex()
    }
  }

  fun sort() {
    Gdx.app.postRunnable {
      synchronized(internalPreviewRendererQueue) {
        if (!Hex.args.mapEditor) {
          islandPreviews.sortBy {
            val authorRoundsToBeat = it.island.authorRoundsToBeat
            if (authorRoundsToBeat == Island.UNKNOWN_ROUNDS_TO_BEAT) Int.MAX_VALUE else authorRoundsToBeat
          }
          Gdx.app.log("LOCAL", "order (artb -> id) ${islandPreviews.map { "${it.island.authorRoundsToBeat} -> ${it.id}" }}")
        }
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
    modifier: PreviewModifier = PreviewModifier.NOTHING,
    onComplete: (preview: FrameBuffer) -> Unit
  ) {
    val runnable = doRenderPreview(island, previewWidth, previewHeight, modifier, onComplete)
    addPreviewRender(runnable)
    renderNextPreview()
  }

  private fun addPreviewRender(runnable: Runnable) {
    synchronized(internalPreviewRendererQueue) {
      renderingCount.incrementAndGet()
      internalPreviewRendererQueue.add(runnable)
    }
  }

  private fun doRenderPreview(
    island: Island,
    previewWidth: Int,
    previewHeight: Int,
    modifier: PreviewModifier = PreviewModifier.NOTHING,
    onComplete: (preview: FrameBuffer) -> Unit
  ) = Runnable {
    val islandScreen = PreviewIslandScreen(-1, island, true)
    islandScreen.resize(previewWidth, previewHeight)
    val buffer = FrameBuffer(
      Pixmap.Format.RGBA8888,
      previewWidth.coerceAtLeast(1),
      previewHeight.coerceAtLeast(1),
      false
    )
    buffer.begin()
    Hex.setClearColorAlpha(0f)
    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or Hex.AA_BUFFER_CLEAR.value)
    screen.updateCamera()
    islandScreen.render(0f)
    screen.camera.setToOrtho(screen.yDown, previewWidth.toFloat(), previewHeight.toFloat())
    val widthOffset = screen.camera.viewportWidth / 5
    val heightOffset = screen.camera.viewportHeight / 5
    screen.batch.use(screen.camera) {
      fun drawAsset(textureRegion: TextureAtlas.AtlasRegion) {
        screen.batch.draw(
          textureRegion,
          widthOffset,
          heightOffset,
          screen.camera.viewportWidth - widthOffset * 2,
          screen.camera.viewportHeight - heightOffset * 2
        )
      }

      fun printText(text: String) {
        val font = Hex.assets.regularFont

        screen.camera.setToOrtho(screen.yDown, widthOffset, heightOffset)
        screen.camera.center(widthOffset, heightOffset)

        screen.batch.projectionMatrix = screen.camera.combined

        font.color = Color.WHITE
        font.draw(
          screen.batch,
          text,
          0f,
          (screen.camera.viewportHeight - font.data.capHeight) / 2f,
          screen.camera.viewportWidth,
          Align.center,
          false
        )
      }

      when (modifier) {
        PreviewModifier.SURRENDER -> drawAsset(Hex.assets.surrender)
        PreviewModifier.LOST -> drawAsset(Hex.assets.grave)
        PreviewModifier.AI_DONE -> drawAsset(Hex.assets.castle)
        PreviewModifier.WON -> printText("${island.round}")
        PreviewModifier.NOTHING -> Unit
      }

      if (Hex.trace && !Hex.args.mapEditor) {
        printText("ARtB: ${island.authorRoundsToBeat}")
      }
    }
    screen.camera.setToOrtho(screen.yDown)
    screen.updateCamera()

    Hex.setClearColorAlpha(1f)

    buffer.end()
    islandScreen.dispose()
    onComplete(buffer)
  }

  fun renderPreviews() {
    reportTiming("loading all island previews") {
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
        if (Hex.args.`update-previews`) {
          updateSelectPreview(id)
          continue
        }
        val runnable = {
          val islandPreviewFile = getIslandFile(id, true)
          val previewProgress = getProgress(id, true)
          val island = loadIslandSync(id)
          try {
            synchronized(internalPreviewRendererQueue) {
              when {
                previewProgress != null -> islandPreviews.add(IslandMetadata(id, island, decodeStringToTexture(previewProgress)))
                islandPreviewFile.exists() -> islandPreviews.add(IslandMetadata(id, island, Texture(islandPreviewFile)))
                else -> {
                  MessagesRenderer.publishWarning("Failed to load preview of island $id")
                  updateSelectPreview(id)
                }
              }
            }
          } catch (e: Exception) {
            MessagesRenderer.publishWarning("Failed to read progress preview of island ${islandPreviewFile.name()}")
            updateSelectPreview(id)
          }
        }
        addPreviewRender(runnable)
        renderNextPreview()
      }
    }
  }

  fun updateSelectPreview(
    id: Int,
    modifier: PreviewModifier = PreviewModifier.NOTHING,
    maybeIsland: Island? = null
  ) {
    KtxAsync.launch(Hex.asyncThread) {
      val metadata = synchronized(internalPreviewRendererQueue) {
        islandPreviews.firstOrNull { it.id == id }
      }
      if (metadata == null) {
        MessagesRenderer.publishWarning("Unknown island with id $id")
        return@launch
      }

      val island = if (maybeIsland == null) {
        val islandFileName = getIslandFileName(id)
        if (!Hex.assets.isLoaded<Island>(islandFileName)) {
          Gdx.app.trace("Update preview") { "Island $id was not loaded, waiting for it to be loaded now..." }
          Hex.assets.load<Island>(islandFileName)
          while (!Hex.assets.update()) {
            Thread.yield()
          }
        }
        Hex.assets.fetch(islandFileName)
      } else {
        maybeIsland
      }

      val rendereredPreviewSize = (2 * shownPreviewSize.toInt()).coerceAtLeast(MIN_PREVIEW_SIZE)
      renderPreview(island, rendereredPreviewSize, rendereredPreviewSize, modifier) { preview ->
        val previewName = getPrefName(id, true)
        if (Hex.args.mapEditor) {
          islandPreferences.remove(previewName)
          val islandPreviewFile = getIslandFile(id, preview = true, allowInternal = false)
          preview.takeScreenshot(islandPreviewFile)
        } else {
          Gdx.app.debug("IS PREVIEW", "Saving preview of island $id")
          islandPreferences.putString(previewName, preview.saveScreenshotAsString())
        }
        islandPreferences.flush()
        val islandMetadata = IslandMetadata(id, island, preview.colorBufferTexture)
        synchronized(internalPreviewRendererQueue) {
          val existingIndex = islandPreviews.indexOfFirst { it.id == id }
          if (existingIndex == -1) {
            islandPreviews.add(islandMetadata)
          } else {
            islandPreviews.get(existingIndex).disposeSafely()
            islandPreviews.set(existingIndex, islandMetadata)
          }
        }
      }
    }
  }

  private fun disposePreviews() {
    synchronized(internalPreviewRendererQueue) {
      for (metadata in islandPreviews) {
        metadata.dispose()
      }
      islandPreviews.clear()
    }
  }

  override fun dispose() {
    Gdx.app.trace("Island Previews") { "Disposing all previews" }
    disposePreviews()
  }

  companion object {
    private const val MIN_PREVIEW_SIZE = 512

    val renderingCount = AtomicInteger(0)
    val renderingPreviews: Boolean get() = renderingCount.get() > 0
  }
}