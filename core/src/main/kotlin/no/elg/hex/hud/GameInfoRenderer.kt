package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
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
import no.elg.hex.hud.ScreenDrawPosition.TOP_CENTER
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.hud.ScreenRenderer.batch
import no.elg.hex.screens.PlayableIslandScreen
import no.elg.hex.util.createHandInstance

/** @author Elg */
class GameInfoRenderer(private val screen: PlayableIslandScreen) : FrameUpdatable {

  private val leftInfo = ArrayList<ScreenText>()

  override fun frameUpdate() {

    ScreenRenderer.drawAll(
      ScreenText("Turn ${screen.island.turn}", bold = true), position = TOP_CENTER
    )
    if (screen.inputProcessor.infiniteMoney) {
      ScreenRenderer.drawAll(emptyText(), CHEATING_SCREEN_TEXT, position = TOP_CENTER)
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

      batch.begin()


      fun calcSize(region: AtlasRegion, heightPercent: Float = 0.1f): Pair<Float, Float> {
        val height = (Gdx.graphics.height * heightPercent)
        val width = height * (region.packedWidth / region.packedHeight.toFloat())
        return width to height
      }

      val castle = Hex.assets.castle
      val (cWidth, buyHeight) = calcSize(castle, 0.075f)
      val peasant = Hex.assets.peasant.getKeyFrame(0f)
      val (pWidth, _) = calcSize(peasant, 0.075f)

      val buyY = Gdx.graphics.height - buyHeight - buyHeight / 3f

      screen.island.inHand?.also { (_, piece) ->
        val region: AtlasRegion =
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

        val (width, height) = calcSize(region)
        val handWidth = height * (Hex.assets.hand.packedWidth / Hex.assets.hand.packedHeight.toFloat())

        batch.draw(Hex.assets.hand, (Gdx.graphics.width - handWidth / 2f) / 2f, (height + height / 2) / 2f, handWidth, height)
        batch.draw(region, Gdx.graphics.width / 2f, height / 2f, width, height)
      }

      val territory = screen.island.selected
      if (territory != null) {
        batch.color = if (territory.capital.balance < CASTLE_COST) HALF_TRANSPARENT_FLOAT_BITS else Color.WHITE
        batch.draw(castle, cWidth / 2f, buyY, cWidth, buyHeight)

        batch.color = if (territory.capital.balance < PEASANT_COST) HALF_TRANSPARENT_FLOAT_BITS else Color.WHITE
        batch.draw(peasant, cWidth + cWidth / 2f + pWidth / 2f, buyY, pWidth, buyHeight)
      }
      batch.end()
    }

    if (Hex.debug) {
      val selected = screen.island.history.historyPointer
      leftInfo.addAll(screen.island.history.historyNotes.mapIndexed { i, it -> ScreenText(it, color = if (i == selected) Color.YELLOW else Color.WHITE) })
    }
    ScreenRenderer.drawAll(*leftInfo.toTypedArray(), position = TOP_RIGHT)
  }

  companion object {
    val CHEATING_SCREEN_TEXT = ScreenText("Cheating enabled!", color = Color.GOLD)
    val CASTLE_COST = Castle::class.createHandInstance().price
    val PEASANT_COST = Peasant::class.createHandInstance().price

    val HALF_TRANSPARENT_FLOAT_BITS = Color(1f, 1f, 1f, 0.5f)
  }
}
