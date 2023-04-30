package no.elg.hex.preview

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import kotlinx.coroutines.launch
import ktx.assets.load
import ktx.async.KtxAsync
import ktx.graphics.center
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.decodeStringToTexture
import no.elg.hex.util.fetch
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.isLoaded
import no.elg.hex.util.reportTiming
import no.elg.hex.util.saveScreenshotAsString
import no.elg.hex.util.takeScreenshot
import no.elg.hex.util.trace
import java.util.concurrent.atomic.AtomicInteger

class IslandPreviewCollection : Disposable {

  private val renderingCount = AtomicInteger(0)
  internal val islandPreviews = Array<Pair<FrameBuffer?, Texture>>()

  val renderingPreviews: Boolean get() = LevelSelectScreen.previews.renderingCount.get() > 0

  fun renderPreview(
    island: Island,
    previewWidth: Int,
    previewHeight: Int,
    modifier: PreviewModifier = PreviewModifier.NOTHING,
    onComplete: (preview: FrameBuffer) -> Unit
  ) {
    renderingCount.incrementAndGet()
    Gdx.app.postRunnable {
      try {
        val islandScreen = PreviewIslandScreen(-1, island)
        islandScreen.resize(previewWidth, previewHeight)
        val buffer = FrameBuffer(Pixmap.Format.RGBA8888, previewWidth.coerceAtLeast(1), previewHeight.coerceAtLeast(1), false)
        buffer.begin()
        Hex.setClearColorAlpha(0f)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or Hex.AA_BUFFER_CLEAR.value)
        LevelSelectScreen.updateCamera()
        islandScreen.render(0f)
        LevelSelectScreen.camera.setToOrtho(LevelSelectScreen.yDown, previewWidth.toFloat(), previewHeight.toFloat())
        val widthOffset = LevelSelectScreen.camera.viewportWidth / 5
        val heightOffset = LevelSelectScreen.camera.viewportHeight / 5
        LevelSelectScreen.batch.use(LevelSelectScreen.camera) {
          fun drawAsset(textureRegion: TextureAtlas.AtlasRegion) {
            LevelSelectScreen.batch.draw(
              textureRegion,
              widthOffset,
              heightOffset,
              LevelSelectScreen.camera.viewportWidth - widthOffset * 2,
              LevelSelectScreen.camera.viewportHeight - heightOffset * 2
            )
          }

          when (modifier) {
            PreviewModifier.SURRENDER -> drawAsset(Hex.assets.surrender)
            PreviewModifier.LOST -> drawAsset(Hex.assets.grave)
            PreviewModifier.AI_DONE -> drawAsset(Hex.assets.castle)

            PreviewModifier.WON -> {
              val text = "${island.round}"

              val font = Hex.assets.regularFont

              LevelSelectScreen.camera.setToOrtho(LevelSelectScreen.yDown, widthOffset, heightOffset)
              LevelSelectScreen.camera.center(widthOffset, heightOffset)

              LevelSelectScreen.batch.projectionMatrix = LevelSelectScreen.camera.combined

              font.color = Color.WHITE
              font.draw(
                LevelSelectScreen.batch,
                text,
                0f,
                (LevelSelectScreen.camera.viewportHeight - font.data.capHeight) / 2f,
                LevelSelectScreen.camera.viewportWidth,
                Align.center,
                false
              )
            }

            PreviewModifier.NOTHING -> Unit
          }
        }
        LevelSelectScreen.camera.setToOrtho(LevelSelectScreen.yDown)
        LevelSelectScreen.updateCamera()

        Hex.setClearColorAlpha(1f)

        buffer.end()
        islandScreen.dispose()
        onComplete(buffer)
      } finally {
        renderingCount.decrementAndGet()
      }
    }
  }

  fun renderPreviews() {
    reportTiming("render all island previews") {
      if (Hex.assets.islandFiles.islandIds.size == 0) {
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
      dispose()

      for (slot in Hex.assets.islandFiles.islandIds) {
        val islandPreviewFile = getIslandFile(slot, true)
        if (Hex.args.`update-previews`) {
          updateSelectPreview(slot, true)
          continue
        }

        val progress = PreviewIslandScreen.getProgress(slot, true)
        when {
          progress != null -> try {
            islandPreviews.add(null to decodeStringToTexture(progress))
          } catch (e: Exception) {
            MessagesRenderer.publishWarning("Failed to read progress preview of island ${islandPreviewFile.name()}")
            updateSelectPreview(slot, false)
          }

          islandPreviewFile.exists() -> islandPreviews.add(null to Texture(islandPreviewFile))
          else -> {
            MessagesRenderer.publishWarning("Failed to read preview of island ${islandPreviewFile.name()}")
            updateSelectPreview(slot, true)
          }
        }
      }
    }
  }

  fun updateSelectPreview(id: Int, save: Boolean, modifier: PreviewModifier = PreviewModifier.NOTHING, island: Island? = null) {
    KtxAsync.launch(Hex.asyncThread) {
      val index = Hex.assets.islandFiles.islandIds.indexOf(id)
      if (index == -1) {
        MessagesRenderer.publishWarning("Failed to find file index of island with a slot at $id")
        return@launch
      }

      val currIsland = if (island == null) {
        val islandFileName = getIslandFileName(id)
        if (!Hex.assets.isLoaded<Island>(islandFileName)) {
          Gdx.app.trace("Update preview", "Island $id was not loaded, waiting for it to be loaded now...")
          val asset = Hex.assets.load<Island>(islandFileName)
          while (!asset.isLoaded()) {
            Thread.yield()
          }
        }
        Hex.assets.fetch(islandFileName)
      } else {
        island
      }

      val rendereredPreviewSize = (2 * LevelSelectScreen.shownPreviewSize.toInt()).coerceAtLeast(MIN_PREVIEW_SIZE)
      renderPreview(currIsland, rendereredPreviewSize, rendereredPreviewSize, modifier) { preview ->
        if (save) {
          val islandPreviewFile = getIslandFile(id, preview = true, allowInternal = false)
          preview.takeScreenshot(islandPreviewFile)
        }
        if (Hex.args.mapEditor) {
          PreviewIslandScreen.islandPreferences.remove(PreviewIslandScreen.getPrefName(id, true))
        } else {
          Gdx.app.debug("IS PREVIEW", "Saving preview of island $id")
          PreviewIslandScreen.islandPreferences.putString(PreviewIslandScreen.getPrefName(id, true), preview.saveScreenshotAsString())
        }
        PreviewIslandScreen.islandPreferences.flush()
        if (index == islandPreviews.size) {
          islandPreviews.add(preview to preview.colorBufferTexture)
        } else {
          islandPreviews.set(index, preview to preview.colorBufferTexture)
        }
      }
    }
  }

  override fun dispose() {
    for (buffer in islandPreviews) {
      buffer.first?.dispose()
      buffer.second.dispose()
    }
    islandPreviews.clear()
  }

  companion object {
    private const val MIN_PREVIEW_SIZE = 512
  }
}