package src.no.elg.hex.rendrer

import com.badlogic.gdx.Gdx
import org.hexworks.mixite.core.api.Hexagon
import src.no.elg.hex.FrameUpdatable
import src.no.elg.hex.InputHandler.cameraOffsetX
import src.no.elg.hex.InputHandler.cameraOffsetY
import src.no.elg.hex.InputHandler.cursorHex
import src.no.elg.hex.InputHandler.mouseX
import src.no.elg.hex.InputHandler.mouseY
import src.no.elg.hex.hexagon.HexUtil
import src.no.elg.hex.hexagon.HexagonData
import src.no.elg.hex.rendrer.HUDRenderer.HUDModus.DEBUG
import src.no.elg.hex.rendrer.HUDRenderer.HUDModus.NONE

/**
 * @author Elg
 */
object HUDRenderer : FrameUpdatable {
  var modus = DEBUG
  override fun frameUpdate() {
    if (modus == NONE) {
      return
    }
    val sr = ScreenRenderer
    if (modus == DEBUG) {
      val fps = String.format("FPS: %4d delta: %.5f", Gdx.graphics.framesPerSecond,
        Gdx.graphics.deltaTime)
      val screenPos = String.format("Screen pos (% 8.2f,% 8.2f)", mouseX, mouseY)
      val realPos = String.format("Real pos (% 8.2f,% 8.2f)", mouseX + cameraOffsetX, mouseY + cameraOffsetY)
      val hex = "Pointing at hex (${cursorHex?.prettyPrint()})"
      sr.begin()
      sr.drawTop(fps, 1)
      sr.drawTop(screenPos, 2)
      sr.drawTop(realPos, 3)
      sr.drawTop(hex, 4)
      sr.end()
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
    return "Hex (${this.center}) type ${HexUtil.getData(this).type.name}"
  }
}
