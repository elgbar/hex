package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import ktx.graphics.center
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.IslandFiles
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.LOST
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.NOTHING
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.SURRENDER
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.WON
import no.elg.hex.screens.PreviewIslandScreen.Companion.getPrefName
import no.elg.hex.screens.PreviewIslandScreen.Companion.getProgress
import no.elg.hex.screens.PreviewIslandScreen.Companion.islandPreferences
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.decodeStringToTexture
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.saveScreenshotAsString
import no.elg.hex.util.takeScreenshot

/** @author Elg */
object LevelSelectScreen : AbstractScreen() {

  private const val PREVIEWS_PER_ROW = 4
  private const val PREVIEW_PADDING_PERCENT = 0.025f
  private const val MIN_PREVIEW_SIZE = 512

  private val NOT_SELECTED_COLOR: Color = Color.LIGHT_GRAY
  private val SELECT_COLOR: Color = Color.GREEN

  private val islandPreviews = Array<Pair<FrameBuffer?, Texture>>()
  private val unprojectVector = Vector3()

  var renderingPreview: Boolean = false
    private set

  val mouseX
    get() = unprojectVector.x
  val mouseY
    get() = unprojectVector.y

  val padding: Float
    get() = Gdx.graphics.width * PREVIEW_PADDING_PERCENT
  private val shownPreviewSize
    get() = (Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * padding) / PREVIEWS_PER_ROW

  val rendereredPreviewSize get() = (2 * shownPreviewSize.toInt()).coerceAtLeast(MIN_PREVIEW_SIZE)

  fun projectCoordinates(inputX: Float, inputY: Float) {
    unprojectVector.x = inputX
    unprojectVector.y = inputY

    camera.unproject(unprojectVector)
  }

  fun renderPreview(island: Island, previewWidth: Int, previewHeight: Int, modifier: PreviewModifier = NOTHING): FrameBuffer {
    val islandScreen = PreviewIslandScreen(-1, island)
    islandScreen.resize(previewWidth, previewHeight)
    val buffer = FrameBuffer(RGBA8888, previewWidth.coerceAtLeast(1), previewHeight.coerceAtLeast(1), false)
    buffer.begin()
    renderingPreview = true
    Hex.setClearColorAlpha(0f)
    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or Hex.AA_BUFFER_CLEAR.value)
    updateCamera()
    islandScreen.render(0f)
    if (modifier != NOTHING) {

      camera.setToOrtho(yDown, previewWidth.toFloat(), previewHeight.toFloat())
      val widthOffset = camera.viewportWidth / 5
      val heightOffset = camera.viewportHeight / 5
      batch.use(camera) {
        when (modifier) {
          SURRENDER -> batch.draw(
            Hex.assets.surrender,
            widthOffset,
            heightOffset,
            camera.viewportWidth - widthOffset * 2,
            camera.viewportHeight - heightOffset * 2
          )
          LOST -> batch.draw(
            Hex.assets.grave,
            widthOffset,
            heightOffset,
            camera.viewportWidth - widthOffset * 2,
            camera.viewportHeight - heightOffset * 2
          )
          WON -> {
            val text = "${island.turn}"

            val font = Hex.assets.regularFont

            camera.setToOrtho(yDown, widthOffset, heightOffset)
            camera.center(widthOffset, heightOffset)

            batch.projectionMatrix = camera.combined

            font.color = WHITE
            font.draw(
              batch,
              text,
              0f,
              (camera.viewportHeight - font.data.capHeight) / 2f,
              camera.viewportWidth,
              Align.center,
              false
            )
          }
          else -> error("Unknown/illegal preview modifier: $modifier")
        }
      }
      camera.setToOrtho(yDown)
      updateCamera()
    }
    Hex.setClearColorAlpha(1f)

    renderingPreview = false
    buffer.end()
    islandScreen.dispose()
    return buffer
  }

  init {
    renderPreviews()
  }

  fun renderPreviews() {
    if (IslandFiles.islandIds.size == 0) {
      if (!Hex.args.`disable-island-loading`) {
        publishWarning("Failed to find any islands to load")
      }
      return
    }
    disposePreviews()

    for (slot in IslandFiles.islandIds) {
      val islandPreviewFile = getIslandFile(slot, true)
      if (Hex.args.`force-update-previews`) {
        updateSelectPreview(slot, true)
        continue
      }

      val progress = getProgress(slot, true)
      when {
        progress != null ->
          try {
            islandPreviews.add(null to decodeStringToTexture(progress))
          } catch (e: Exception) {
            publishWarning("Failed to read progress preview of island ${islandPreviewFile.name()}")
            updateSelectPreview(slot, false)
          }
        islandPreviewFile.exists() -> islandPreviews.add(null to Texture(islandPreviewFile))
        else -> {
          publishWarning("Failed to read preview of island ${islandPreviewFile.name()}")
          updateSelectPreview(slot, true)
        }
      }
    }
  }

  fun updateSelectPreview(slot: Int, save: Boolean, modifier: PreviewModifier = NOTHING, island: Island? = null) {
    val index = IslandFiles.islandIds.indexOf(slot)
    if (index == -1) {
      publishWarning("Failed to find file index of island with a slot at $slot")
      return
    }

    val islandFileName = getIslandFileName(slot)

    if (!Hex.assets.isLoaded(islandFileName)) {
      Hex.assets.load(islandFileName, Island::class.java)
    }
    val currIsland = island ?: Hex.assets.finishLoadingAsset(islandFileName)

    val preview = renderPreview(currIsland, rendereredPreviewSize, rendereredPreviewSize, modifier)
    if (save) {
      val islandPreviewFile = getIslandFile(slot, preview = true, allowInternal = false)
      preview.takeScreenshot(islandPreviewFile)
    }
    if (Hex.args.mapEditor) {
      islandPreferences.remove(getPrefName(slot, true))
    } else {
      Gdx.app.debug("IS PREVIEW", "Saving preview of island $slot")
      islandPreferences.putString(getPrefName(slot, true), preview.saveScreenshotAsString())
    }
    islandPreferences.flush()
    if (index == islandPreviews.size) {
      islandPreviews.add(preview to preview.colorBufferTexture)
    } else {
      islandPreviews.set(index, preview to preview.colorBufferTexture)
    }
  }

  fun rect(index: Int): Rectangle {
    val gridX = index % PREVIEWS_PER_ROW
    val gridY = index / PREVIEWS_PER_ROW

    val size = this.shownPreviewSize

    return Rectangle(padding + (padding + size) * gridX, padding + (padding + size) * gridY, size, size)
  }

  private fun drawBox(x: Float, y: Float, width: Float, height: Float) {
    lineRenderer.color = if (mouseX in x..x + width && mouseY in y..y + height) SELECT_COLOR else NOT_SELECTED_COLOR
    lineRenderer.rect(x, y, width, height)
  }

  override fun render(delta: Float) {
    projectCoordinates(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())

    lineRenderer.begin(Line)
    batch.begin()
    for ((i, preview) in islandPreviews.withIndex()) {
      val (x, y, width, height) = rect(i)
      batch.draw(preview.second, x, y, width, height)
      drawBox(x, y, width, height)
    }
    batch.end()

    if (Hex.args.mapEditor) {
      val (x, y, width, height) = rect(islandPreviews.size)

      lineRenderer.line(
        x + width / 2f,
        y + height / 2f + height / 10f,
        x + width / 2f,
        y + height / 2f - height / 10f,
        WHITE,
        WHITE
      )
      lineRenderer.line(
        x + width / 2f + width / 10f,
        y + height / 2f,
        x + width / 2f - width / 10f,
        y + height / 2f,
        WHITE,
        WHITE
      )
      drawBox(x, y, width, height)
    }

    lineRenderer.end()
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(LevelSelectInputProcessor)
  }

  override fun hide() = Unit

  override fun dispose() {
    super.dispose()
    disposePreviews()
  }

  private fun disposePreviews() {
    for (buffer in islandPreviews) {
      buffer.first?.dispose()
      buffer.second.dispose()
    }
    islandPreviews.clear()
  }

  enum class PreviewModifier {
    NOTHING,
    SURRENDER,
    WON,
    LOST
  }
}
