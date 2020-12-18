package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import ktx.collections.plusAssign
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.IslandFiles
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.getIslandFileName

/** @author Elg */
object LevelSelectScreen : AbstractScreen() {

  private const val PREVIEWS_PER_ROW = 5
  private const val PREVIEW_PADDING_PERCENT = 0.025f
  private const val MIN_PREVIEW_SIZE = 512

  val NOT_SELECTED_COLOR = Color.LIGHT_GRAY
  val SELECT_COLOR = Color.GREEN

  private val islandPreviews = Array<FrameBuffer>()
  private val unprojectVector = Vector3()

  val mouseX
    get() = unprojectVector.x
  val mouseY
    get() = unprojectVector.y

  val padding: Float
    get() = Gdx.graphics.width * PREVIEW_PADDING_PERCENT
  private val previewSize
    get() = (Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * padding) / PREVIEWS_PER_ROW

  fun renderPreview(island: Island, previewWidth: Int, previewHeight: Int): FrameBuffer {
    val islandScreen = PreviewIslandScreen(-1, island)
    islandScreen.resize(previewWidth, previewHeight)
    val buffer =
      FrameBuffer(RGBA8888, previewWidth.coerceAtLeast(1), previewHeight.coerceAtLeast(1), false)
    buffer.begin()
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or Hex.AA_BUFFER_CLEAR.value)
    camera.update()
    islandScreen.render(0f)
    buffer.end()
    islandScreen.dispose()
    return buffer
  }

  fun renderPreviews() {
    for (buffer in islandPreviews) {
      buffer.dispose()
    }
    islandPreviews.clear()

    if (!Hex.assets.isLoaded(getIslandFileName(0))) {
      if (!Hex.args.`disable-island-loading`) {
        publishWarning("Failed to find any islands to load")
      }
      return
    }
    val previewSize = (2 * this.previewSize.toInt()).coerceAtLeast(MIN_PREVIEW_SIZE)

    for (slot in IslandFiles.islandIds) {
      val file = getIslandFileName(slot)
      val island = Hex.assets.finishLoadingAsset<Island>(file)
      islandPreviews += renderPreview(island, previewSize, previewSize)
    }
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(LevelSelectInputProcessor)
  }

  override fun hide() {
    Hex.inputMultiplexer.removeProcessor(LevelSelectInputProcessor)
    for (preview in islandPreviews) {
      preview.dispose()
    }
    islandPreviews.clear()
  }

  fun rect(index: Int): Rectangle {
    val gridX = index % PREVIEWS_PER_ROW
    val gridY = index / PREVIEWS_PER_ROW

    val size = this.previewSize

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
      batch.draw(preview.colorBufferTexture, x, y, width, height)
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
    renderPreviews()
  }
}
