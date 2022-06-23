package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.api.Resizable

object DebugGraphRenderer : FrameUpdatable, Disposable, Resizable {

  const val RELATIVE_GRAPH_WIDTH = 0.25f

  private lateinit var fbo: FrameBuffer
  private var batch = SpriteBatch()

  private var clearFbo = true
  private var fboWidth: Int = 0
  private var fboHeight: Int = 0

  private var fpsIndex: Int = 0 // frames per second

  private const val COL_WIDTH = 2
  private const val COLOR_SIZE = 6

  private val colors = Array<Color>(COLOR_SIZE) {
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

  private var fpsDeltaAcc = 0f

  val isEnabled: Boolean get() = Settings.enableDebugFPSGraph && Hex.debug

  override fun frameUpdate() {
    fpsDeltaAcc += Gdx.graphics.deltaTime
    val updateFps = Gdx.graphics.frameId % COL_WIDTH == 0L

    if (updateFps) {
      begin()
      drawFps()
      end()
    }
    batch.begin()
    batch.draw(fbo.colorBufferTexture, 0f, fboHeight.toFloat())
    batch.end()
  }

  private fun begin() {
    fbo.begin()
    if (clearFbo) {
      Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
      clearFbo = false
    }
    // https://www.khronos.org/opengl/wiki/Scissor_Test
    Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
  }

  private fun end() {
    Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    fbo.end()
    Hex.resetClearColor()
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
    val color = colors.getOrNull(colorIndex) ?: Color.RED

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

  override fun resize(width: Int, height: Int) {
    batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    fboWidth = width
    fboHeight = height / 3
    fpsIndex = Int.MAX_VALUE

    if (::fbo.isInitialized) {
      fbo.dispose()
    }
    fbo = FrameBuffer(Pixmap.Format.RGBA4444, fboWidth, fboHeight, false)
    clearFbo = true
  }

  override fun dispose() {
    if (::fbo.isInitialized) {
      fbo.dispose()
    }
  }
}
