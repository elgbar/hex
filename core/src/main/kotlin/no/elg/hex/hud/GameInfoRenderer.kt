package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
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
import no.elg.hex.hud.ScreenDrawPosition.BOTTOM_LEFT
import no.elg.hex.hud.ScreenDrawPosition.TOP_CENTER
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.hud.ScreenRenderer.batch
import no.elg.hex.screens.PlayableIslandScreen

/** @author Elg */
class GameInfoRenderer(private val playableIslandScreen: PlayableIslandScreen) : FrameUpdatable {

  override fun frameUpdate() {

    ScreenRenderer.drawAll(
      ScreenText("Turn ${playableIslandScreen.island.turn}", bold = true), position = TOP_CENTER
    )
    if (playableIslandScreen.inputProcessor.infiniteMoney) {
      ScreenRenderer.drawAll(CHEATING_SCREEN_TEXT, position = BOTTOM_LEFT)
    }

    if (playableIslandScreen.island.currentAI == null) {

      playableIslandScreen.island.selected?.also { selected ->
        val list =
          mutableListOf(
            ScreenText(
              "Treasury: ",
              next = signColoredText(selected.capital.balance) { "%d".format(it) }
            ),
            ScreenText(
              "Estimated income: ",
              next = signColoredText(selected.income) { "%+d".format(it) }
            )
          )
        if (Hex.debug) {
          list +=
            ScreenText(
              "Holding: ",
              next = nullCheckedText(playableIslandScreen.island.inHand, color = Color.YELLOW)
            )
        }
        ScreenRenderer.drawAll(*list.toTypedArray(), position = TOP_RIGHT)
      }

      playableIslandScreen.island.inHand?.also { (_, piece) ->
        batch.begin()
        val region =
          when (piece) {
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

        val height = (Gdx.graphics.height * 0.1f)
        val width = height * (region.packedWidth / region.packedHeight.toFloat())

        val handWidth =
          height * (Hex.assets.hand.packedWidth / Hex.assets.hand.packedHeight.toFloat())

        batch.draw(
          Hex.assets.hand,
          (Gdx.graphics.width - handWidth / 2f) / 2f,
          (height + height / 2) / 2f,
          handWidth,
          height
        )
        batch.draw(region, Gdx.graphics.width / 2f, height / 2f, width, height)
        batch.end()
      }
    }
  }

  companion object {
    val CHEATING_SCREEN_TEXT = ScreenText("Cheating enabled!", color = Color.GOLD)
  }
}
