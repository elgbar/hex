package no.elg.hex.hud

import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT

/**
 * @author Elg
 */
object GameInfoRenderer : FrameUpdatable {

  override fun frameUpdate() {
    Hex.island.selected?.also { selected ->
      ScreenRenderer.drawAll(
        ScreenText("Treasury: ", next = signColoredText(selected.capital.balance) { "%+d".format(it) }),
        ScreenText("Estimated income: ", next = signColoredText(selected.income) { "%+d".format(it) }),
        position = TOP_RIGHT)
    }
  }
}
