package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import no.elg.hex.Hex
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4

/** @author Elg */
class LevelSelectScreen : AbstractScreen(), ReloadableScreen {

  internal val input by lazy { LevelSelectInputProcessor(this) }
  private val mouseX get() = input.mouseX
  private val mouseY get() = input.mouseY

  override fun render(delta: Float) {
    if (LevelSelectInputProcessor.lastY != camera.position.y) {
      input.restoreScrollPosition()
    }
    lineRenderer.begin(Line)
    batch.begin()

    val (_, sy, _, sheight) = input.slotRect(0, NON_ISLAND_SCALE, 0f)

    // Draw the first row of non-islands
    if (sy + sheight > camera.position.y - camera.viewportHeight / 2f) {
      drawScreenSprite(Hex.assets.settingsDown, Hex.assets.settings, 0)
      drawScreenSprite(Hex.assets.helpDown, Hex.assets.help, PREVIEWS_PER_ROW - 1)

      if (Hex.args.mapEditor) {
        drawLevelCreationIcon()
      }
    }

    for ((i, metadata) in Hex.assets.islandPreviews.islandWithIndex()) {
      val (x, y, width, height) = input.slotRect(i + PREVIEWS_PER_ROW)

      if (y + height < camera.position.y - camera.viewportHeight / 2f) {
        // island is above camera, no need to render
        continue
      }

      if (y > camera.position.y + camera.viewportHeight / 2f) {
        // the island is below the camera, no need to render further
        break
      }

      batch.draw(metadata.preview, x, y, width, height)
      if (Hex.debugStage || metadata.island.authorRoundsToBeat == Island.UNKNOWN_ROUNDS_TO_BEAT) {
        drawBox(x, y, width, height)
      }
    }
    batch.end()
    lineRenderer.end()
  }

  fun color(x: Float, y: Float, width: Float, height: Float, notSelectedColor: Color = NOT_SELECTED_COLOR): Color {
    return if (mouseX in x..x + width && mouseY in y..y + height) SELECT_COLOR else notSelectedColor
  }

  private fun drawBox(x: Float, y: Float, width: Float, height: Float, notSelectedColor: Color = NOT_SELECTED_COLOR) {
    lineRenderer.color = color(x, y, width, height, notSelectedColor)
    lineRenderer.rect(x, y, width, height)
  }

  private fun drawSlotBox(index: Int) {
    val (inputX, inputY, inputWidth, inputHeight) = input.slotRect(index, 1f)
    drawBox(inputX, inputY, inputWidth, inputHeight, Color.GRAY)
  }

  private fun drawScreenSprite(selected: TextureRegion, notSelected: TextureRegion, index: Int) {
    val (x, y, width, height) = input.slotRect(index, NON_ISLAND_SCALE)
    val sprite = if (mouseX in x..x + width && mouseY in y..y + height) selected else notSelected
    batch.draw(sprite, x, y, width, height)
    if (Hex.debugStage) {
      // Show the interaction area of these buttons
      drawSlotBox(index)
    }
  }

  private fun drawLevelCreationIcon() {
    val newMapIndex = 1
    val (x, y, width, height) = input.slotRect(newMapIndex, NON_ISLAND_SCALE)

    drawBox(x, y, width, height)
    // Draw crosshair in the center of the box
    val color = color(x, y, width, height)
    lineRenderer.line(
      x + width / 2f,
      y + height / 2f + height / 10f,
      x + width / 2f,
      y + height / 2f - height / 10f,
      color,
      color
    )
    lineRenderer.line(
      x + width / 2f + width / 10f,
      y + height / 2f,
      x + width / 2f - width / 10f,
      y + height / 2f,
      color,
      color
    )

    if (Hex.debugStage) {
      drawSlotBox(newMapIndex)
    }
  }

  override fun recreate(): AbstractScreen {
    return LevelSelectScreen()
  }

  override fun show() {
    input.show()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    input.restoreScrollPosition()
  }

  companion object {
    const val NON_ISLAND_SCALE = 0.45f
    const val PREVIEWS_PER_ROW = 4 // We currently use three slots, don't decrease to two :p
    private val NOT_SELECTED_COLOR: Color = Color.LIGHT_GRAY
    private val SELECT_COLOR: Color = Color.GREEN
    private const val PREVIEW_PADDING_PERCENT = 0.025f

    val paddingX: Float
      get() = Gdx.graphics.width * PREVIEW_PADDING_PERCENT
    val paddingY: Float get() = paddingX * 1.5f
    val shownPreviewSize
      get() = (Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * paddingX) / PREVIEWS_PER_ROW
  }
}