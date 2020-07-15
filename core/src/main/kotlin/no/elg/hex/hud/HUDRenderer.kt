package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hud.HUDRenderer.HUDModus.DEBUG
import no.elg.hex.hud.HUDRenderer.HUDModus.NONE
import no.elg.hex.hud.ScreenRenderer.drawAll
import no.elg.hex.input.InputHandler.MAX_ZOOM
import no.elg.hex.input.InputHandler.MIN_ZOOM
import no.elg.hex.input.InputHandler.cursorHex
import no.elg.hex.input.InputHandler.mouseX
import no.elg.hex.input.InputHandler.mouseY
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

/**
 * @author Elg
 */
object HUDRenderer : FrameUpdatable {
  var modus = DEBUG
  override fun frameUpdate() {
    if (modus == NONE) {
      return
    }
    if (modus == DEBUG) {
      val fps = String.format("FPS: %4d delta: %.5f zoom: ", Gdx.graphics.framesPerSecond,
        Gdx.graphics.deltaTime)
      val screenPos = String.format("Screen pos (% 4d,% 4d)", Gdx.input.x, Gdx.input.y)
      val realPos = String.format("Real pos (% 8.2f,% 8.2f)", mouseX, mouseY)

      drawAll(screenTexts = *arrayOf(
        ScreenText(fps, next = validatedText(Hex.camera.zoom, MIN_ZOOM, MAX_ZOOM)),
        ScreenText(screenPos),
        ScreenText(realPos),
        ScreenText("Pointing at hex ", next = nullCheckedText(cursorHex?.prettyPrint())
        )))
    }
    //        else {
//            sr.begin();
//        }
  }

  /**
   * How much information to show
   */
  enum class HUDModus {
    DEBUG, NORMAL, NONE
  }

  private fun Hexagon<HexagonData>.prettyPrint(): String {
    return "${getData().team.name} shape ${this.getData().type.name}"
  }
}
