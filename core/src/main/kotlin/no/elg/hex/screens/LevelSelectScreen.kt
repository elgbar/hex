package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array as GdxArray
import no.elg.hex.Assets.Companion.ISLAND_FILE_ENDING
import no.elg.hex.Assets.Companion.ISLAND_SAVES_DIR
import no.elg.hex.Hex
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4

/** @author Elg */
object LevelSelectScreen : AbstractScreen() {

  private const val FRAME_BUFFER_SIZE = 1024

  private const val PREVIEWS_PER_ROW = 3
  private const val PREVIEW_PADDING_PERCENT = 0.025f

  private val NOT_SELECTED_COLOR = Color.LIGHT_GRAY
  private val SELECT_COLOR = Color.GREEN

  private val islandPreviews = GdxArray<FrameBuffer>()

  private val unprojectVector = Vector3()

  val previews
    get() = islandPreviews.size

  val mouseX
    get() = unprojectVector.x
  val mouseY
    get() = unprojectVector.y

  fun renderPreviews() {
    for (buffer in islandPreviews) {
      buffer.dispose()
    }
    islandPreviews.clear()

    if (!Hex.assets.isLoaded(getIslandFileName(0))) {
      play(0)
      return
    }

    for (slot in 0..Int.MAX_VALUE) {
      val file = LevelSelectScreen.getIslandFile(slot)
      if (file.exists()) {
        if (file.isDirectory) continue

        val islandScreen = IslandScreen(slot, Hex.assets.get(getIslandFileName(slot)), false)
        islandScreen.resize(FRAME_BUFFER_SIZE, FRAME_BUFFER_SIZE)

        val buffer = FrameBuffer(RGBA8888, FRAME_BUFFER_SIZE, FRAME_BUFFER_SIZE, false)
        buffer.begin()
        islandScreen.render(0f)
        buffer.end()
        islandPreviews.add(buffer)
        islandScreen.dispose()
      } else {
        break
      }
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

    val padding: Float = Gdx.graphics.width * PREVIEW_PADDING_PERCENT
    val size: Float = (Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * padding) / PREVIEWS_PER_ROW

    return Rectangle(
        padding + (padding + size) * gridX, padding + (padding + size) * gridY, size, size)
  }

  override fun render(delta: Float) {
    camera.update()

    batch.projectionMatrix = camera.combined
    lineRenderer.projectionMatrix = camera.combined

    unprojectVector.x = Gdx.input.x.toFloat()
    unprojectVector.y = Gdx.input.y.toFloat()

    camera.unproject(unprojectVector)

    lineRenderer.begin(Line)
    batch.begin()
    for ((i, preview) in islandPreviews.withIndex()) {
      val (x, y, width, height) = rect(i)
      batch.draw(preview.colorBufferTexture, x, y, width, height)

      lineRenderer.color =
          (if (mouseX in x..x + width && mouseY in y..y + height) SELECT_COLOR
          else NOT_SELECTED_COLOR)
      lineRenderer.rect(x, y, width, height)
    }
    batch.end()
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
