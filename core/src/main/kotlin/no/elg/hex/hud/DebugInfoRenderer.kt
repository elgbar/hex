package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hud.ScreenRenderer.drawAll
import no.elg.hex.input.BasicInputHandler.MAX_ZOOM
import no.elg.hex.input.BasicInputHandler.MIN_ZOOM
import no.elg.hex.input.BasicInputHandler.cursorHex
import no.elg.hex.input.BasicInputHandler.mouseX
import no.elg.hex.input.BasicInputHandler.mouseY
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

/**
 * @author Elg
 */
object DebugInfoRenderer : FrameUpdatable {

  override fun frameUpdate() {

    val screenPos = String.format("Screen pos (% 4d,% 4d)", Gdx.input.x, Gdx.input.y)
    val realPos = String.format("Real pos (% 8.2f,% 8.2f)", mouseX, mouseY)
    drawAll(
      ScreenText("FPS: ",
        next = validatedText(Gdx.graphics.framesPerSecond, 30, Int.MAX_VALUE, color = Color.YELLOW, format = { "%4d".format(it) },
          next = ScreenText(" zoom: ",
            next = validatedText(Hex.camera.zoom, MIN_ZOOM, MAX_ZOOM) { "%.2f".format(it) }))),
      ScreenText(screenPos),
      ScreenText(realPos),
      ScreenText("Pointing at hex ", next = nullCheckedText(cursorHex?.prettyPrint(), color = Color.YELLOW))
    )
  }

  private fun Hexagon<HexagonData>.prettyPrint(): String {
    return "${getData().team.name} shape ${this.getData().type.name}"
  }
}
