package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Disposable
import ktx.assets.disposeSafely
import ktx.collections.component1
import ktx.collections.component2
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.event.HexagonChangedTeamEvent
import no.elg.hex.event.SimpleEventListener
import no.elg.hex.hud.ScreenDrawPosition.TOP_LEFT
import no.elg.hex.hud.ScreenRenderer.draw
import no.elg.hex.hud.ScreenRenderer.drawAll
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.getData

/** @author Elg */
class DebugInfoRenderer(private val islandScreen: PreviewIslandScreen) : FrameUpdatable, Disposable {

  private val listener: SimpleEventListener<HexagonChangedTeamEvent>
  private val fpsText: ScreenText
  private val debugLines: Array<ScreenText>
  private val teamPercent: MutableList<String> = mutableListOf()
  private val teamHexagons: MutableList<String> = mutableListOf()

  init {

    fpsText = variableText(
      "FPS: ",
      Gdx.graphics::getFramesPerSecond,
      10,
      Int.MAX_VALUE,
      format = { "%4d".format(it) },
      next = prefixText(" Delta (ms) ", Gdx.graphics::getDeltaTime) { "%.4f".format(it) }
    )

    if (Hex.debug) {

      val basicInputHandler = islandScreen.basicIslandInputProcessor

      debugLines = arrayOf(
        prefixText(
          "Island is ",
          callable = { islandScreen.island.grid.gridData },
          format = { gridData -> "${gridData.gridWidth} x ${gridData.gridHeight} ${gridData.gridLayout.getName()}" }
        ),
        prefixText(
          "Current team is ",
          islandScreen.island::currentTeam
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
          "Pointing at hex ",
          next =
          nullCheckedText(
            callable = basicInputHandler::cursorHex,
            color = Color.YELLOW,
            format = { cursorHex ->
              "( %2d, % 2d) ${islandScreen.island.getData(cursorHex)}".format(cursorHex.gridX, cursorHex.gridZ)
            }
          )
        ),
        prefixText("Team hexagons    ", ::teamHexagons),
        prefixText("Team percentages ", ::teamPercent),
        StaticScreenText(
          "Territory ",
          next =
          nullCheckedText(
            callable = islandScreen.island::selected,
            color = Color.YELLOW,
            format = { territory -> "Size: ${territory.hexagons.count()} Bordering Enemies ${territory.enemyBorderHexes.size}" }
          )
        ),
        prefixText(
          "Total Hexagons ",
          islandScreen.island.allHexagons::size,
          next = prefixText(" (visible ", islandScreen.island.visibleHexagons::size, next = END_PARENTHESIS)
        )
      )
    } else {
      debugLines = emptyArray()
    }

    fun updatePercentages() {
      teamPercent.clear()
      teamHexagons.clear()
      islandScreen.island.calculatePercentagesHexagons()
        .mapTo(teamPercent) { (team, percent) -> "$team ${"%2d".format((percent * 100).toInt())}%" }.sort()
      islandScreen.island.hexagonsPerTeam.mapTo(teamHexagons) { (team, hexes) -> "$team ${"%3d".format(hexes)}" }.sort()
    }

    listener = SimpleEventListener.create { updatePercentages() }

    updatePercentages()
  }

  override fun frameUpdate() {
    if (Settings.showFps || Hex.debug && Settings.enableDebugHUD) {
      ScreenRenderer.begin()
      fpsText.draw(1, TOP_LEFT)
      ScreenRenderer.end()
    }
    if (Hex.debug && Settings.enableDebugHUD) {
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