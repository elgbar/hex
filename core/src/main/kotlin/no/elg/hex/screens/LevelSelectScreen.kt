package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array as GdxArray
import no.elg.hex.Assets.Companion.ISLAND_FILE_ENDING
import no.elg.hex.Assets.Companion.ISLAND_SAVES_DIR
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4

/** @author Elg */
object LevelSelectScreen : AbstractScreen() {

  private const val PREVIEWS_PER_ROW = 5
  private const val PREVIEW_PADDING_PERCENT = 0.025f

  private val NOT_SELECTED_COLOR = Color.LIGHT_GRAY
  private val SELECT_COLOR = Color.GREEN

  private val islandPreviews = GdxArray<FrameBuffer>()

  private val unprojectVector = Vector3()

  var islandAmount = islandPreviews.size
    private set
  val mouseX
    get() = unprojectVector.x
  val mouseY
    get() = unprojectVector.y


  private val previewSize get() = ((Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * (Gdx.graphics.width * PREVIEW_PADDING_PERCENT)) / PREVIEWS_PER_ROW).toInt()

  private fun renderPreviews() {
    for (buffer in islandPreviews) {
      buffer.dispose()
    }
    islandPreviews.clear()

    if (!Hex.assets.isLoaded(getIslandFileName(0))) {
      publishMessage("Failed to find any islands to load, generating a new island")
      play(0)
      return
    }
    val previewSize = this.previewSize

    for (slot in 0..Int.MAX_VALUE) {
      val file = getIslandFile(slot)
      if (file.exists()) {
        if (file.isDirectory) continue

        val fileName = getIslandFileName(slot)

        if (!Hex.assets.isLoaded(fileName, Island::class.java)) {
          Hex.assets.load(fileName, Island::class.java)
        }
        val islandScreen = IslandScreen(slot, Hex.assets.finishLoadingAsset(fileName), false)
        islandScreen.resize(previewSize, previewSize)
        val buffer = FrameBuffer(RGBA8888, previewSize, previewSize, false)
        buffer.begin()
        islandScreen.render(0f)
        buffer.end()
        islandPreviews.add(buffer)
        islandScreen.dispose()
      } else {
        break
      }
    }
    islandAmount = islandPreviews.size
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

    val padding: Float = Gdx.graphics.width * PREVIEW_PADDING_PERCENT
    val size: Float = (Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * padding) / PREVIEWS_PER_ROW

    return Rectangle(
        padding + (padding + size) * gridX, padding + (padding + size) * gridY, size, size)
  }

  private fun drawBox(x: Float, y: Float, width: Float, height: Float) {
    lineRenderer.color =
        if (mouseX in x..x + width && mouseY in y..y + height) SELECT_COLOR else NOT_SELECTED_COLOR
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
          Color.WHITE)
      lineRenderer.line(
          x + width / 2f + width / 10f,
          y + height / 2f,
          x + width / 2f - width / 10f,
          y + height / 2f,
          Color.WHITE,
          Color.WHITE)
      drawBox(x, y, width, height)
    }

    lineRenderer.end()
  }

  fun getIslandFileName(slot: Int) = "$ISLAND_SAVES_DIR/island-$slot.$ISLAND_FILE_ENDING"

  fun getIslandFile(slot: Int): FileHandle {
    val path = getIslandFileName(slot)
    val internal = Gdx.files.internal(path)
    return if (internal.exists()) internal else Gdx.files.local(path)
  }

  fun play(id: Int) {
    play(id, Hex.assets.get(getIslandFileName(id)))
  }

  fun play(id: Int, island: Island) {
    Gdx.app.postRunnable { Hex.screen = IslandScreen(id, island) }
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    renderPreviews()
  }
}
