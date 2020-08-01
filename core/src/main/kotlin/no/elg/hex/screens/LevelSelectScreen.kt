package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap.Format.RGBA4444
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import no.elg.hex.Assets.Companion.ISLAND_FILE_ENDING
import no.elg.hex.Assets.Companion.ISLAND_SAVES_DIR
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer.publishMessage
import no.elg.hex.hud.ScreenText
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.island.Island
import org.hexworks.mixite.core.api.HexagonalGridLayout.RECTANGULAR
import com.badlogic.gdx.utils.Array as GdxArray

/**
 * @author Elg
 */
object LevelSelectScreen : AbstractScreen() {

  private const val FRAME_BUFFER_SIZE = 512

  private const val PREVIEWS_PER_ROW = 3
  private const val PREVIEW_PADDING_PERCENT = 0.025f

  private val NOT_SELECTED_COLOR = Color.WHITE
  private val SELECT_COLOR = Color.GREEN

  private val islandPreviews = GdxArray<FrameBuffer>()
  private val batch: SpriteBatch by lazy { SpriteBatch() }
  private val lineRenderer: ShapeRenderer = ShapeRenderer(1000)

  private val unprojectVector = Vector3()

  val previews get() = islandPreviews.size

  val mouseX get() = unprojectVector.x
  val mouseY get() = unprojectVector.y

  override fun show() {
    Hex.inputMultiplexer.addProcessor(LevelSelectInputProcessor)
    val islandDir = Gdx.files.local(ISLAND_SAVES_DIR)
    if (!islandDir.isDirectory) {
      play(0)
    }
    for ((i, saveFile: FileHandle) in islandDir.list(ISLAND_FILE_ENDING).withIndex()) {
      val island = loadIsland(saveFile) ?: continue
      val islandScreen = IslandScreen(i, island, false)
      val buffer = FrameBuffer(RGBA4444, FRAME_BUFFER_SIZE, FRAME_BUFFER_SIZE, false)
      buffer.begin()
      islandScreen.render(0f)
      buffer.end()
      islandPreviews.add(buffer)
      islandScreen.dispose()
    }
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
    val paddingH: Float = Gdx.graphics.height * PREVIEW_PADDING_PERCENT
    val width: Float = (Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * padding) / PREVIEWS_PER_ROW
    val height: Float = (Gdx.graphics.height - (1 + PREVIEWS_PER_ROW) * paddingH) / PREVIEWS_PER_ROW

    return Rectangle(
      padding + (padding + width) * gridX,
      padding + (padding + height) * gridY,
      width,
      height
    )
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

      lineRenderer.color = (if (mouseX in x..x + width && mouseY in y..y + height) SELECT_COLOR else NOT_SELECTED_COLOR)
      lineRenderer.rect(x, y, width, height)
    }
    batch.end()
    lineRenderer.end()
  }


  fun getIslandFile(slot: Int): FileHandle = Gdx.files.local("$ISLAND_SAVES_DIR/island-$slot.$ISLAND_FILE_ENDING")

  fun play(id: Int) {
    val island = loadIsland(getIslandFile(id)) ?: Island(40, 25, RECTANGULAR)
    play(id, island)
  }

  fun play(id: Int, island: Island) {

    publishMessage("Successfully loaded island $id")

    Gdx.app.postRunnable { Hex.screen = IslandScreen(id, island) }
  }

  fun loadIsland(file: FileHandle): Island? {
    val json: String = try {
      requireNotNull(file.readString())
    } catch (e: Exception) {
      publishMessage(ScreenText("Failed to load island the name '${file.name()}'", color = Color.RED))
      return null
    }

    return try {
      Island.deserialize(json)
    } catch (e: Exception) {
      publishMessage(ScreenText("Invalid island save data for island '${file.name()}'", color = Color.RED))
      Gdx.app.debug("LOAD", e.message)
      null
    }
  }

}
