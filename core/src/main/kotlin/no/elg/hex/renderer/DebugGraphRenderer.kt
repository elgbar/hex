package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.api.Resizable

class DebugGraphRenderer : ScissorRenderer(), Resizable {

  private var fpsIndex: Int = 0 // frames per second

  private var fpsDeltaAcc = 0f

  override fun frameUpdate() {
    fpsDeltaAcc += Gdx.graphics.deltaTime
    val updateFps = Gdx.graphics.frameId % COL_WIDTH == 0L
    if (updateFps) {
      use {
        drawFps()
      }
    }
    drawFbo(0f, fboHeight.toFloat())
  }

  private fun drawFps() {
    if (fpsIndex >= fboWidth * RELATIVE_GRAPH_WIDTH) {
      fpsIndex = 0
    } else {
      fpsIndex += COL_WIDTH
    }

    val fpsDelta = (fpsDeltaAcc / COL_WIDTH) * 10_000f
    fpsDeltaAcc = 0f

    drawColumn(fpsDelta, fpsIndex)
  }

  private fun drawColumn(height: Float, index: Int) {
    // inverse height
    val pillarHeight = (fboHeight - height.toInt()).coerceAtLeast(0)

    val colorIndex = (height / fboHeight * COLOR_SIZE.toFloat()).toInt()
    val color = colors.getOrNull(colorIndex) ?: Color.BLUE

    // 0 is top, height is bottom, pillarHeight is how far up the pillar should go
    // Fill from the top to pillarHeight with transparent pixels
    Gdx.gl.glScissor(index, 0, COL_WIDTH * 2, fboHeight)
    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    // Then fill the rest (from pillarHeight to bottom) with colored pixels
    Gdx.gl.glScissor(index, pillarHeight, COL_WIDTH, fboHeight)
    Gdx.gl.glClearColor(color.r, color.g, color.b, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
  }

  override fun calculateFboWidth(width: Int, height: Int): Int = width
  override fun calculateFboHeight(width: Int, height: Int): Int = height / 3

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    fpsIndex = Int.MAX_VALUE
  }

  companion object {

    val isEnabled: Boolean get() = Settings.enableDebugFPSGraph && Hex.debug
    private const val RELATIVE_GRAPH_WIDTH = 0.25f

    private const val COL_WIDTH = 2
    private const val COLOR_SIZE = 6

    val colors = Array<Color>(COLOR_SIZE) {
      when (it) {
        0 -> Color.GREEN
        1 -> Color.LIME
        2 -> Color.YELLOW
        3 -> Color.GOLD
        4 -> Color.ORANGE
        5 -> Color.RED
        else -> Color.BLUE
      }
    }
  }
}