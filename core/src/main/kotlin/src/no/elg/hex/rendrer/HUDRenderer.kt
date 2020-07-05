package src.no.elg.hex.rendrer

import com.badlogic.gdx.Gdx
import src.no.elg.hex.FrameUpdatable
import src.no.elg.hex.InputHandler.cursorHex
import src.no.elg.hex.InputHandler.mouseX
import src.no.elg.hex.InputHandler.mouseY
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
      val pointing = String.format("Pointing pos (% 8.2f,% 8.2f)", mouseX, mouseY)
      val hex = "Pointing at hex ($cursorHex)"
      sr.begin()
      sr.drawTop(fps, 1)
      sr.drawTop(pointing, 2)
      sr.drawTop(hex, 3)
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
}
