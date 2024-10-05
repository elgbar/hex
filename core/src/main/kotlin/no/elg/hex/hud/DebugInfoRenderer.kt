package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Disposable
import ktx.assets.disposeSafely
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.event.SimpleEventListener
import no.elg.hex.event.events.HexagonChangedTeamEvent
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.strengthToTypeOrNull
import no.elg.hex.hud.ScreenDrawPosition.TOP_LEFT
import no.elg.hex.hud.ScreenRenderer.draw
import no.elg.hex.hud.ScreenRenderer.drawAll
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.getData
import kotlin.collections.component1
import kotlin.collections.component2

/** @author Elg */
class DebugInfoRenderer(private val islandScreen: PreviewIslandScreen) : FrameUpdatable, Disposable {

  private var listener: SimpleEventListener<HexagonChangedTeamEvent>? = null
  private val debugLines: Array<ScreenText>
  private val teamPercent: MutableList<String> = mutableListOf()
  private val teamHexagons: MutableList<String> = mutableListOf()
  private val fpsText: ScreenText = variableText(
    "FPS: ",
    Gdx.graphics::getFramesPerSecond,
    10,
    Int.MAX_VALUE,
    format = { "%4d".format(it) },
    next = prefixText(" Delta (ms) ", Gdx.graphics::getDeltaTime) { "%.4f".format(it) }
  )

  private val island get() = islandScreen.island

  init {
    val basicInputHandler = islandScreen.basicIslandInputProcessor

    val mapEditorInfo = arrayOf(
      prefixText(
        "Island ",
        callable = ::island,
        format = { island ->
          val gridData = island.grid.gridData
          "${gridData.gridWidth} x ${gridData.gridHeight} ${gridData.gridLayout.getName()}"
        },
        next = prefixText(
          " ARtB ",
          islandScreen.metadata::authorRoundsToBeat,
          next = prefixText(
            " id ",
            islandScreen.metadata::id,
            next = prefixText(
              " for testing ",
              islandScreen.metadata::forTesting
            )
          )
        )
      ),
      StaticScreenText(
        "Pointing at hex ",
        next =
        nullCheckedText(
          callable = basicInputHandler::cursorHex,
          color = Color.YELLOW,
          format = { cursorHex ->
            val data = island.getData(cursorHex)
            "( %2d, % 2d) $data".format(cursorHex.gridX, cursorHex.gridZ)
          },
          next = StaticScreenText(
            " in territory? ",
            next = booleanText(
              callable = { basicInputHandler.cursorHex?.let { island.isInTerritory(it) } == true },
              next = prefixNullableText(
                prefix = " strength ",
                callable = basicInputHandler::cursorHex,
                format = { cursorHex ->
                  val data = island.getData(cursorHex)
                  val str = island.calculateStrength(cursorHex, data.team)
                  val strType = strengthToTypeOrNull(str) ?: Empty::class
                  "${strType.simpleName} ($str)"
                },
                next = prefixNullableText(
                  " estimated income ",
                  callable = { island.findTerritory(basicInputHandler.cursorHex) },
                  format = { territory -> territory.income.toString() }
                )
              )
            )
          )
        )
      ),
      prefixText("Team hexagons    ", ::teamHexagons),
      prefixText("Team percentages ", ::teamPercent),
      prefixText(
        "Total Hexagons ",
        island.allHexagons::size,
        next = prefixText(" (visible ", island.visibleHexagons::size, next = END_PARENTHESIS)
      )
    )

    if (Hex.debug) {
      debugLines = arrayOf(
        prefixText(
          "Current team is ",
          island::currentTeam
        ),
        prefixText(
          "Screen pos (",
          { "%4d,%4d".format(Gdx.input.x, Gdx.input.y) },
          next = END_PARENTHESIS
        ),
        prefixText(
          "Real pos (",
          { "% 5.0f,% 5.0f".format(basicInputHandler.mouseX, basicInputHandler.mouseY) },
          next = END_PARENTHESIS
        ),
        StaticScreenText(
          "Territory ",
          next =
          nullCheckedText(
            callable = island::selected,
            color = Color.YELLOW,
            format = { territory -> "Size: ${territory.hexagons.count()} Bordering Enemies ${territory.enemyBorderHexes.size}" }
          )
        ),
        *mapEditorInfo
      )
    } else if (Hex.mapEditor) {
      debugLines = mapEditorInfo
    } else {
      debugLines = emptyArray()
    }
    updatePercentages()
  }

  private fun updatePercentages() {
    teamPercent.clear()
    teamHexagons.clear()
    island.calculatePercentagesHexagons().mapTo(teamPercent) { (team, percent) -> "$team ${"%2d".format((percent * 100).toInt())}%" }.sort()
    island.hexagonsPerTeam.mapTo(teamHexagons) { (team, hexes) -> "$team ${"%3d".format(hexes)}" }.sort()
  }

  override fun frameUpdate() {
    if (listener == null) {
      listener = SimpleEventListener.create { updatePercentages() }
    }
    val showAll = (Hex.debug || Hex.mapEditor) && Settings.enableDebugHUD
    if (Settings.showFps || showAll) {
      ScreenRenderer.use {
        fpsText.draw(1, TOP_LEFT)
      }
    }
    if (showAll) {
      drawAll(*debugLines, lineOffset = 2)
    }
  }

  override fun dispose() {
    listener.disposeSafely()
  }

  companion object {
    val END_PARENTHESIS = StaticScreenText(")")
  }
}