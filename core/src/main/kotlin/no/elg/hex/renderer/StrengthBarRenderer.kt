package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
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
import no.elg.hex.hexagon.SimpleEventListener
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.TeamChangeHexagonDataEvent
import no.elg.hex.island.Island
import kotlin.math.roundToInt

class StrengthBarRenderer(val island: Island) : FrameUpdatable, Disposable, Resizable {

  private val listener: SimpleEventListener<TeamChangeHexagonDataEvent>
  private lateinit var fbo: FrameBuffer
  private var batch = SpriteBatch()

  private var clearFbo = true
  private var fboWidth: Int = 0
  private var fboHeight: Int = 0

  override fun frameUpdate() {
    batch.begin()
    batch.draw(
      fbo.colorBufferTexture,
      (Gdx.graphics.width) / 2f - (fboWidth / 2f),
      (Gdx.graphics.height) - fboHeight * 2f
    )
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

  fun redrawBar() {
    begin()
    val percentagesHexagons = island.calculatePercentagesHexagons()
    // inverse height
    var offsetX = 0
    for (team in Team.values()) {
      val percent = percentagesHexagons[team] ?: error("No percentage for team $team")
      offsetX += drawRect(offsetX, percent, team)
    }
    end()
  }

  private fun drawRect(xOffset: Int, percent: Float, team: Team): Int {
    val color = team.color
    val endX = (fboWidth * percent).roundToInt()

    Gdx.gl.glScissor(xOffset, 0, xOffset + endX, fboHeight)
    Gdx.gl.glClearColor(color.r, color.g, color.b, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    return endX
  }

  override fun resize(width: Int, height: Int) {
    batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())

    val (larger, smaller) = if (width > height) {
      Pair(width, height)
    } else {
      Pair(height, width)
    }

    fboWidth = (larger * RELATIVE_GRAPH_LARGER_SIZE).toInt().coerceAtLeast(1)
    fboHeight = (smaller * RELATIVE_GRAPH_SMALLER_SIZE).toInt().coerceAtLeast(1)

    if (::fbo.isInitialized) {
      fbo.dispose()
    }
    fbo = FrameBuffer(Pixmap.Format.RGBA4444, fboWidth, fboHeight, false)
    clearFbo = true
    redrawBar()
  }

  override fun dispose() {
    if (::fbo.isInitialized) {
      fbo.dispose()
    }
  }

  init {
    listener = SimpleEventListener.create {
      redrawBar()
    }
  }

  companion object {
    private const val RELATIVE_GRAPH_LARGER_SIZE = 0.2f
    private const val RELATIVE_GRAPH_SMALLER_SIZE = 0.05f

    val isEnabled: Boolean get() = Settings.enableStrengthBar
  }
}