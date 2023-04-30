package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import no.elg.hex.Hex
import no.elg.hex.input.LevelSelectInputProcessor
import no.elg.hex.util.component1
import no.elg.hex.util.component2
import no.elg.hex.util.component3
import no.elg.hex.util.component4
import no.elg.hex.util.isLazyInitialized

/** @author Elg */
class LevelSelectScreen : AbstractScreen(), ReloadableScreen {

  internal val input by lazy { LevelSelectInputProcessor(this) }
  private val mouseX get() = input.mouseX
  private val mouseY get() = input.mouseY

  private fun drawBox(x: Float, y: Float, width: Float, height: Float) {
    lineRenderer.color = if (mouseX in x..x + width && mouseY in y..y + height) SELECT_COLOR else NOT_SELECTED_COLOR
    lineRenderer.rect(x, y, width, height)
  }

  override fun render(delta: Float) {
    lineRenderer.begin(Line)
    batch.begin()

    val (sx, sy, swidth, sheight) = input.rect(0, NON_ISLAND_SCALE, 0f)
    if (sy + sheight > camera.position.y - camera.viewportHeight / 2f) {
      val settingsSprite = if (mouseX in sx..sx + swidth && mouseY in sy..sy + sheight) Hex.assets.settingsDown else Hex.assets.settings
      batch.draw(settingsSprite, sx, sy, swidth, sheight)

      val (hx, hy, hwidth, hheight) = input.rect(PREVIEWS_PER_ROW - 1, NON_ISLAND_SCALE, 1f)
      val helpSprite = if (mouseX in hx..hx + hwidth && mouseY in hy..hy + hheight) Hex.assets.helpDown else Hex.assets.help
      batch.draw(helpSprite, hx, hy, hwidth, hheight)

      if (Hex.args.mapEditor) {
        val (x, y, width, height) = input.rect(1, NON_ISLAND_SCALE)

        drawBox(x, y, width, height)
        val color = if (mouseX in x..x + width && mouseY in y..y + height) SELECT_COLOR else NOT_SELECTED_COLOR
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
      }
    }

    for ((i, preview) in Hex.assets.islandPreviews.islandPreviews.withIndex()) {
      val (x, y, width, height) = input.rect(i + PREVIEWS_PER_ROW)

      if (y + height < camera.position.y - camera.viewportHeight / 2f) {
        // island is above camera, no need to render
        continue
      }

      if (y > camera.position.y + camera.viewportHeight / 2f) {
        // the island is below the camera, no need to render further
        break
      }

      batch.draw(preview.second, x, y, width, height)
      drawBox(x, y, width, height)
    }
    batch.end()

    lineRenderer.end()
  }

  override fun recreate(): AbstractScreen {
    return LevelSelectScreen().also {
      if (::input.isLazyInitialized) {
        it.input.lastY = input.lastY
      }
    }
  }

  override fun show() {
    input.show()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    input.restoreScrollPosition()
  }

  companion object {
    const val NON_ISLAND_SCALE = 0.4f
    const val PREVIEWS_PER_ROW = 4
    private val NOT_SELECTED_COLOR: Color = Color.LIGHT_GRAY
    private val SELECT_COLOR: Color = Color.GREEN
    private const val PREVIEW_PADDING_PERCENT = 0.025f

    val padding: Float
      get() = Gdx.graphics.width * PREVIEW_PADDING_PERCENT
    val shownPreviewSize
      get() = (Gdx.graphics.width - (1 + PREVIEWS_PER_ROW) * padding) / PREVIEWS_PER_ROW
  }
}