package no.elg.hex.hud

import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.screens.IslandScreen

/**
 * @author Elg
 */
class GameInfoRenderer(private val islandScreen: IslandScreen, gameInputProcessor: GameInputProcessor) : FrameUpdatable {

  override fun frameUpdate() {
    islandScreen.island.selected?.also { selected ->
      ScreenRenderer.drawAll(
        ScreenText("Treasury: ", next = signColoredText(selected.capital.balance) { "%+d".format(it) }),
        ScreenText("Estimated income: ", next = signColoredText(selected.income) { "%+d".format(it) }),
        position = TOP_RIGHT)
    }
  }
}
