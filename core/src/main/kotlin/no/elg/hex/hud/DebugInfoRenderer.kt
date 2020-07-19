package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.drawAll
import no.elg.hex.input.BasicInputHandler.MAX_ZOOM
import no.elg.hex.input.BasicInputHandler.MIN_ZOOM
import no.elg.hex.input.BasicInputHandler.cursorHex
import no.elg.hex.input.BasicInputHandler.mouseX
import no.elg.hex.input.BasicInputHandler.mouseY
import no.elg.hex.util.getData

/**
 * @author Elg
 */
object DebugInfoRenderer : FrameUpdatable {

  override fun frameUpdate() {

    val screenPos = "Screen pos (%4d,%4d)".format(Gdx.input.x, Gdx.input.y)
    val realPos = "Real pos (% 5.0f,% 5.0f)".format(mouseX, mouseY)
    drawAll(
      ScreenText("FPS: ",
        next = validatedText(Gdx.graphics.framesPerSecond, 30, Int.MAX_VALUE, color = Color.YELLOW, format = { "%4d".format(it) },
          next = ScreenText(" zoom: ",
            next = validatedText(Hex.camera.zoom, MIN_ZOOM, MAX_ZOOM) { "%.2f".format(it) }))),
      ScreenText(screenPos),
      ScreenText(realPos),
      ScreenText("Pointing at hex ", next = nullCheckedText(cursorHex?.getData(), color = Color.YELLOW))
    )
  }
}
