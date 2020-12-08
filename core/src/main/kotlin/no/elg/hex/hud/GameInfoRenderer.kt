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
import no.elg.hex.hexagon.HexagonData
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
class GameInfoRenderer(private val screen: PlayableIslandScreen) : FrameUpdatable {

  private val leftInfo = ArrayList<ScreenText>()

  override fun frameUpdate() {

    ScreenRenderer.drawAll(
      ScreenText("Turn ${screen.island.turn}", bold = true), position = TOP_CENTER
    )
    if (screen.inputProcessor.infiniteMoney) {
      ScreenRenderer.drawAll(CHEATING_SCREEN_TEXT, position = BOTTOM_LEFT)
    }

    leftInfo.clear()

    if (screen.island.currentAI == null) {

      screen.island.selected?.also { selected ->
        leftInfo += ScreenText("Treasury: ", next = signColoredText(selected.capital.balance) { "%d".format(it) })
        leftInfo += ScreenText("Estimated income: ", next = signColoredText(selected.income) { "%+d".format(it) })
        if (Hex.debug) {
          leftInfo += emptyText()
          leftInfo += ScreenText("Holding: ", next = nullCheckedText(screen.island.inHand, color = Color.YELLOW))
          leftInfo += ScreenText("Holding edge piece: ", next = booleanText(screen.island.inHand?.piece?.data === HexagonData.EDGE_DATA))
          leftInfo += emptyText()
        }
      }

      screen.island.inHand?.also { (_, piece) ->
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

    if (Hex.debug) {
      val selected = screen.island.history.historyPointer
      leftInfo.addAll(screen.island.history.historyNotes.mapIndexed { i, it -> ScreenText(it, color = if (i == selected) Color.YELLOW else Color.WHITE) })
    }
    ScreenRenderer.drawAll(*leftInfo.toTypedArray(), position = TOP_RIGHT)
  }

  companion object {
    val CHEATING_SCREEN_TEXT = ScreenText("Cheating enabled!", color = Color.GOLD)
  }
}
