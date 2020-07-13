package src.no.elg.hex.hud

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import org.hexworks.mixite.core.api.Hexagon
import src.no.elg.hex.api.FrameUpdatable
import src.no.elg.hex.hexagon.HexUtil.getData
import src.no.elg.hex.hexagon.HexagonData
import src.no.elg.hex.hud.HUDRenderer.HUDModus.DEBUG
import src.no.elg.hex.hud.HUDRenderer.HUDModus.NONE
import src.no.elg.hex.hud.ScreenRenderer.drawAll
import src.no.elg.hex.input.InputHandler.MAX_ZOOM
import src.no.elg.hex.input.InputHandler.MIN_ZOOM
import src.no.elg.hex.input.InputHandler.cursorHex
import src.no.elg.hex.input.InputHandler.mouseX
import src.no.elg.hex.input.InputHandler.mouseY

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
    return "Hex (${this.center}) type ${this.getData().type.name}"
  }
}
