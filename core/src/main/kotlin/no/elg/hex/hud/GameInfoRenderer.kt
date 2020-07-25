package no.elg.hex.hud

import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT

/**
 * @author Elg
 */
object GameInfoRenderer : FrameUpdatable {

  override fun frameUpdate() {
    Hex.island.selected?.also { (capital, hexagons) ->

      val income = capital.income(hexagons)

      val incomeColor = when {
        income > 0 -> Color.GREEN
        income < 0 -> Color.RED
        else -> Color.BLACK
      }

      ScreenRenderer.drawAll(
        ScreenText("Treasury: ${capital.balance}"),
        ScreenText("Income: ", next = ScreenText("%+d".format(income), color = incomeColor))
        , position = TOP_RIGHT)
    }
  }
}
