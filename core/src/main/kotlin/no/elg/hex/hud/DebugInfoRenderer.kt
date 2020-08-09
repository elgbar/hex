package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.drawAll
import no.elg.hex.input.BasicIslandInputProcessor.Companion.MAX_ZOOM
import no.elg.hex.input.BasicIslandInputProcessor.Companion.MIN_ZOOM
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData

/** @author Elg */
class DebugInfoRenderer(private val islandScreen: IslandScreen) : FrameUpdatable {

  override fun frameUpdate() {

    val basicInputHandler = islandScreen.basicIslandInputProcessor

    val screenPos = "Screen pos (%4d,%4d)".format(Gdx.input.x, Gdx.input.y)
    val realPos =
        "Real pos (% 5.0f,% 5.0f)".format(basicInputHandler.mouseX, basicInputHandler.mouseY)
    val cursorHex = basicInputHandler.cursorHex
    val cursorData = if (cursorHex != null) islandScreen.island.getData(cursorHex) else null
    drawAll(
        variableText(
            "FPS: ",
            Gdx.graphics.framesPerSecond,
            30,
            Int.MAX_VALUE,
            format = { "%4d".format(it) },
            next =
                variableText(
                    " zoom: ",
                    islandScreen.camera.zoom,
                    MIN_ZOOM,
                    MAX_ZOOM,
                    format = { "%.2f".format(it) })),
        ScreenText(
            "Island is ${islandScreen.island.grid.gridData.gridWidth} x ${islandScreen.island.grid.gridData.gridHeight} ${islandScreen.island.grid.gridData.gridLayout}"),
        ScreenText("Current team is ${islandScreen.island.currentTeam}"),
        ScreenText(screenPos),
        ScreenText(realPos),
        ScreenText(
            "Pointing at hex ",
            next =
                nullCheckedText(
                    cursorData,
                    color = Color.YELLOW,
                    format = { "( %2d, % 2d) $it".format(cursorHex?.gridX, cursorHex?.gridZ) })))
  }
}
