package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Grave
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.Spearman
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.hud.ScreenRenderer.batch
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.screens.IslandScreen

/**
 * @author Elg
 */
class GameInfoRenderer(private val islandScreen: IslandScreen, gameInputProcessor: GameInputProcessor) : FrameUpdatable {


  override fun frameUpdate() {
    islandScreen.island.selected?.also { selected ->
      ScreenRenderer.drawAll(
        ScreenText("Treasury: ", next = signColoredText(selected.capital.balance) { "%d".format(it) }),
        ScreenText("Estimated income: ", next = signColoredText(selected.income) { "%+d".format(it) }),
        ScreenText("Holding: ", next = nullCheckedText(islandScreen.island.inHand)),
        position = TOP_RIGHT)
    }

    islandScreen.island.inHand?.also { (_, piece) ->
      batch.begin()
      val region = when (piece) {
        is Capital -> Hex.assets.capital
        is PalmTree -> Hex.assets.palm
        is PineTree -> Hex.assets.pine
        is Castle -> Hex.assets.castle
        is Grave -> Hex.assets.grave
        is Peasant -> Hex.assets.peasant.getKeyFrame(0f)
        is Spearman -> Hex.assets.spearman.getKeyFrame(0f)
        is Knight -> Hex.assets.knight.getKeyFrame(0f)
        is Baron -> Hex.assets.baron.getKeyFrame(0f)
        is Empty -> return@also
      }

      region.flip(false, true)

      batch.draw(
        region,
        Gdx.graphics.width / 2f,
        Gdx.graphics.height - region.packedHeight * 2.5f,
        0f,
        0f,
        region.packedWidth.toFloat(),
        region.packedHeight.toFloat(),
        2f,
        2f,
        0f
      )

      region.flip(false, true)
      batch.end()

    }
  }
}
