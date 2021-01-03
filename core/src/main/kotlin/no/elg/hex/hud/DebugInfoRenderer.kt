package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.drawAll
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.getData

/** @author Elg */
class DebugInfoRenderer(private val islandScreen: PreviewIslandScreen) : FrameUpdatable {

  private val lines: Array<ScreenText>

  init {

    val fpsText = variableText(
      "FPS: ",
      Gdx.graphics::getFramesPerSecond,
      10,
      Int.MAX_VALUE,
      format = { "%4d".format(it) },
      next = prefixText(" Delta (ms) ", Gdx.graphics::getDeltaTime) { "%.3f".format(it) }
    )

    if (Hex.debug) {

      val basicInputHandler = islandScreen.basicIslandInputProcessor

      lines = arrayOf(
        emptyText(),
        fpsText,
        prefixText(
          "Island is ",
          callable = { islandScreen.island.grid.gridData },
          format = { gridData -> "${gridData.gridWidth} x ${gridData.gridHeight} ${gridData.gridLayout}" }
        ),
        prefixText(
          "Current team is ", islandScreen.island::currentTeam
        ),
        prefixText(
          "Screen pos (", { "%4d,%4d".format(Gdx.input.x, Gdx.input.y) }, next = StaticScreenText(")")
        ),
        prefixText(
          "Real pos (", { "% 5.0f,% 5.0f".format(basicInputHandler.mouseX, basicInputHandler.mouseY) }, next = StaticScreenText(")")
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
        )
      )
    } else {
      lines = arrayOf(emptyText(), fpsText)
    }
  }

  override fun frameUpdate() {
    if (!Hex.debug && !Settings.showFps) return

    drawAll(*lines)
  }
}
