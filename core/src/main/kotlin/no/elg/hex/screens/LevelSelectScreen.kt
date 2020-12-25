package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.IslandFiles
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.LOST
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.NOTHING
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.SURRENDER
import no.elg.hex.screens.LevelSelectScreen.PreviewModifier.WON
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.takeScreenshot

/** @author Elg */
object LevelSelectScreen : AbstractScreen() {

  private var fontSize: Int = Hex.assets.fontSize

  private const val PREVIEWS_PER_ROW = 5
  private const val PREVIEW_PADDING_PERCENT = 0.025f
  private const val MIN_PREVIEW_SIZE = 512

  val NOT_SELECTED_COLOR = Color.LIGHT_GRAY
  val SELECT_COLOR = Color.GREEN

  private val islandPreviews = Array<Pair<FrameBuffer?, Texture>>()
  private val unprojectVector = Vector3()

  private var winFont: BitmapFont? = null

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

  fun renderPreview(island: Island, previewWidth: Int, previewHeight: Int, modifier: PreviewModifier = NOTHING): FrameBuffer {
    val islandScreen = PreviewIslandScreen(-1, island)
    islandScreen.resize(previewWidth, previewHeight)
    val buffer = FrameBuffer(RGBA8888, previewWidth.coerceAtLeast(1), previewHeight.coerceAtLeast(1), false)
    buffer.begin()
    renderingPreview = true
    Hex.setClearColorAlpha(0f)
    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or Hex.AA_BUFFER_CLEAR.value)
    camera.update()
    islandScreen.render(0f)
    if (modifier != NOTHING) {

      val widthOffset = camera.viewportWidth / 6
      val heightOffset = camera.viewportHeight / 6
      batch.begin()

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
          val font = Hex.assets.getFont(bold = false, italic = false, flip = true, fontSize = fontSize)
          if (winFont == null) {
            winFont = font
          }
          font.draw(
            batch,
            text,
            (camera.viewportWidth - (text.length * font.spaceXadvance) / 2f) / 2f,
            (camera.viewportHeight - font.lineHeight) / 2f
          )
        }
        else -> error("Unknown/illegal preview modifier: $modifier")
      }
      batch.end()
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

    for (slot in IslandFiles.islandIds) {
      val islandPreviewFile = getIslandFile(slot, true)
      if (!Hex.args.`force-update-previews` && islandPreviewFile.exists()) {
        islandPreviews.add(null to Texture(islandPreviewFile))
      } else {
        if (!Hex.args.`force-update-previews`) {
          publishWarning("Failed to read preview of island ${islandPreviewFile.name()}")
        }
        updateSelectPreview(slot, true)
      }
    }
  }

  fun updateSelectPreview(slot: Int, save: Boolean, modifier: PreviewModifier = NOTHING) {
    val index = IslandFiles.islandIds.indexOf(slot)
    if (index == -1) {
      publishWarning("Failed to find file index of island with a slot at $slot")
      return
    }

    val islandFileName = getIslandFileName(slot)

    if (!Hex.assets.isLoaded(islandFileName)) {
      Hex.assets.load(islandFileName, Island::class.java)
    }
    val island = Hex.assets.finishLoadingAsset<Island>(islandFileName)

    val preview = renderPreview(island, rendereredPreviewSize, rendereredPreviewSize, modifier)
    if (save) {
      val islandPreviewFile = getIslandFile(slot, preview = true, allowInternal = false)
      preview.takeScreenshot(islandPreviewFile)
    }
    if (index == islandPreviews.size) {
      islandPreviews.add(preview to preview.colorBufferTexture)
    } else {
      islandPreviews.set(index, preview to preview.colorBufferTexture)
    }
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(LevelSelectInputProcessor)
  }

  override fun hide() {
    Hex.inputMultiplexer.removeProcessor(LevelSelectInputProcessor)
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

    unprojectVector.x = Gdx.input.x.toFloat()
    unprojectVector.y = Gdx.input.y.toFloat()

    camera.unproject(unprojectVector)

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
        Color.WHITE,
        Color.WHITE
      )
      lineRenderer.line(
        x + width / 2f + width / 10f,
        y + height / 2f,
        x + width / 2f - width / 10f,
        y + height / 2f,
        Color.WHITE,
        Color.WHITE
      )
      drawBox(x, y, width, height)
    }

    lineRenderer.end()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    fontSize = camera.viewportWidth.toInt() / 5
    Hex.assets.loadFont(bold = false, italic = false, flip = true, fontSize = fontSize)
  }

  override fun dispose() {
    super.dispose()
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
