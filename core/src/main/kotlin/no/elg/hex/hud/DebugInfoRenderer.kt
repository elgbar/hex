package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.drawAll
import no.elg.hex.input.BasicIslandInputProcessor.Companion.MAX_ZOOM
import no.elg.hex.input.BasicIslandInputProcessor.Companion.MIN_ZOOM
import no.elg.hex.screens.PlayableIslandScreen
import no.elg.hex.util.getData

/** @author Elg */
class DebugInfoRenderer(private val playableIslandScreen: PlayableIslandScreen) : FrameUpdatable {

  override fun frameUpdate() {

    val basicInputHandler = playableIslandScreen.basicIslandInputProcessor

    val screenPos = "%4d,%4d".format(Gdx.input.x, Gdx.input.y)
    val realPos = "% 5.0f,% 5.0f".format(basicInputHandler.mouseX, basicInputHandler.mouseY)
    val cursorHex = basicInputHandler.cursorHex
    val cursorData = if (cursorHex != null) playableIslandScreen.island.getData(cursorHex) else null
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
          playableIslandScreen.camera.zoom,
          MIN_ZOOM,
          MAX_ZOOM,
          format = { "%.2f".format(it) })),
      ScreenText(
        "Island is ",
        next =
        ScreenText(
          "${playableIslandScreen.island.grid.gridData.gridWidth} x ${playableIslandScreen.island.grid.gridData.gridHeight} ${playableIslandScreen.island.grid.gridData.gridLayout}",
          color = Color.YELLOW)),
      ScreenText(
        "Current team is ",
        next = ScreenText(playableIslandScreen.island.currentTeam, color = Color.YELLOW)),
      ScreenText(
        "Screen pos (",
        next = ScreenText(screenPos, color = Color.YELLOW, next = ScreenText(")"))),
      ScreenText(
        "Real pos (", next = ScreenText(realPos, color = Color.YELLOW, next = ScreenText(")"))),
      ScreenText(
        "Pointing at hex ",
        next =
        nullCheckedText(
          cursorData,
          color = Color.YELLOW,
          format = { "( %2d, % 2d) $it".format(cursorHex?.gridX, cursorHex?.gridZ) })))
  }
}
