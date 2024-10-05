package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.event.SimpleEventListener
import no.elg.hex.event.events.HexagonChangedTeamEvent
import no.elg.hex.event.events.HexagonVisibilityChanged
import no.elg.hex.event.events.TeamEndTurnEvent
import no.elg.hex.hexagon.Team
import no.elg.hex.island.Island
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class StrengthBarRenderer(val island: Island) : ScissorRenderer() {

  private val hexagonChangeListener: SimpleEventListener<HexagonChangedTeamEvent>
  private val endTurnListener: SimpleEventListener<TeamEndTurnEvent>
  private val visibilityChangedListener: SimpleEventListener<HexagonVisibilityChanged>?

  override fun frameUpdate() {
    drawFbo((Gdx.graphics.width) / 2f - (fboWidth / 2f), (Gdx.graphics.height) - fboHeight * 2f)
  }

  private fun redrawBar() {
    use {
      val percentagesHexagons = island.calculatePercentagesHexagons()
      // inverse height
      var offsetX = 0
      for (team in Team.entries) {
        val percent = percentagesHexagons[team] ?: error("No percentage for team $team")
        offsetX += drawRect(offsetX, percent, team, island.currentTeam == team)
      }
    }
    Gdx.graphics.requestRendering()
  }

  private fun drawRect(xOffset: Int, percent: Float, team: Team, currentTeam: Boolean): Int {
    val color = team.color
    val endX = (fboWidth * percent).roundToInt()

    if (currentTeam && !Hex.mapEditor) {
      Gdx.gl.glScissor(xOffset, (fboHeight * RELATIVE_HIGHLIGHT_BORDER_PERCENT).toInt(), xOffset + endX, fboHeight)
      val highlight = Color.WHITE
      Gdx.gl.glClearColor(highlight.r, highlight.g, highlight.b, 1f)
      Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

      Gdx.gl.glScissor(xOffset, 0, xOffset + endX, (fboHeight * RELATIVE_HIGHLIGHT_BORDER_INVERSE_PERCENT).toInt())
    } else {
      Gdx.gl.glScissor(xOffset, 0, xOffset + endX, fboHeight)
    }
    Gdx.gl.glClearColor(color.r, color.g, color.b, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    return endX
  }

  override fun calculateFboWidth(width: Int, height: Int): Int {
    val larger = max(width, height)
    return (larger * RELATIVE_GRAPH_LARGER_SIZE).toInt()
  }

  override fun calculateFboHeight(width: Int, height: Int): Int {
    val smaller = min(width, height)
    return (smaller * RELATIVE_GRAPH_SMALLER_SIZE).toInt()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    redrawBar()
  }

  override fun dispose() {
    super.dispose()
    hexagonChangeListener.dispose()
    endTurnListener.dispose()
    visibilityChangedListener?.dispose()
  }

  init {
    hexagonChangeListener = SimpleEventListener.create { redrawBar() }
    endTurnListener = SimpleEventListener.create { redrawBar() }
    visibilityChangedListener = if (Hex.mapEditor) {
      SimpleEventListener.create {
        redrawBar()
      }
    } else {
      null
    }
  }

  companion object {
    private const val RELATIVE_GRAPH_LARGER_SIZE = 0.2f
    private const val RELATIVE_GRAPH_SMALLER_SIZE = 0.05f
    private const val RELATIVE_HIGHLIGHT_BORDER_PERCENT = 0.10f
    private const val RELATIVE_HIGHLIGHT_BORDER_INVERSE_PERCENT = 1f - RELATIVE_HIGHLIGHT_BORDER_PERCENT

    val isEnabled: Boolean get() = Settings.enableStrengthBar
  }
}