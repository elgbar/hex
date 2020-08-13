package no.elg.hex.ai

import com.badlogic.gdx.Gdx
import kotlin.random.Random.Default as random
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.Spearman
import no.elg.hex.hexagon.Team
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.util.canAttack
import no.elg.hex.util.createInstance
import no.elg.hex.util.getData
import no.elg.hex.util.getTerritories
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon

/**
 * An AI that does random actions for a random amount of time. All action it can take will have some
 * value. For example it will not click on an empty hexagon in a territory when it does not hold
 * anything in it's hand.
 *
 * This AI operates independently on each territory in the given team. It has no long term goals.
 *
 * @author Elg
 */
@ExperimentalStdlibApi
class RandomAI(override val team: Team) : AI {

  private val buyablePieces =
      arrayOf(
          Castle::class,
          Peasant::class,
          Spearman::class,
          Knight::class,
          Baron::class,
          Peasant::class,
          Spearman::class)

  fun pickUp(territory: Territory, gameInputProcessor: GameInputProcessor): Boolean {
    Gdx.app.trace("RAI-$team", "Picking up a piece")
    if (territory.island.inHand?.piece != null) {
      Gdx.app.trace("RAI-$team", "Already holding a piece! (${territory.island.inHand?.piece})")
      return true
    }

    // hexagons we can pick up pieces from
    val pickUpHexes =
        HashSet<Hexagon<HexagonData>>(
            territory.hexagons.filter {
              val piece = territory.island.getData(it).piece
              piece is LivingPiece && !piece.moved
            })

    // only buy if there are no more units to move
    if (pickUpHexes.isEmpty()) {
      Gdx.app.trace(
          "RAI-$team", "No pieces to pick up. Buying a unit (balance ${territory.capital.balance})")
      val piece = buyablePieces.filter { territory.capital.canBuy(it) }.randomOrNull()
      if (piece == null) {
        Gdx.app.trace(
            "RAI-$team",
            "Cannot afford any pieces in this territory, I only have ${territory.capital.balance}")
        return false
      }
      Gdx.app.trace("RAI-$team", "Buying the unit ${piece.simpleName} ")
      gameInputProcessor.buyUnit(piece.createInstance(HexagonData.EDGE_DATA))
      Gdx.app.trace("RAI-$team", "New balance ${territory.capital.balance}")
    } else {
      Gdx.app.trace("RAI-$team", "There is something to pick up in the current territory!")
      gameInputProcessor.click(pickUpHexes.random())
    }
    Gdx.app.trace("RAI-$team", "I am now holding ${territory.island.inHand?.piece}")
    return true
  }

  fun place(territory: Territory, gameInputProcessor: GameInputProcessor) {
    Gdx.app.trace("RAI-$team", "Placing held piece")
    val handPiece = territory.island.inHand?.piece
    if (handPiece == null) {
      Gdx.app.trace("RAI-$team", "Not holding any piece!")
      return
    }
    if (handPiece is Castle) {
      // hexagons where we can place castles
      val placableHexes =
          HashSet<Hexagon<HexagonData>>(
              territory.hexagons.filter {
                val piece = territory.island.getData(it).piece
                !(piece is Castle || piece is Capital || piece is LivingPiece)
              })

      val hexagon = placableHexes.randomOrNull() ?: return
      gameInputProcessor.click(hexagon)
    } else if (handPiece is LivingPiece) {

      // hexagon where we can put a living piece
      val attackableHexes =
          HashSet<Hexagon<HexagonData>>(
              territory.hexagons.filter {
                val piece = territory.island.getData(it).piece
                !((piece is LivingPiece && !piece.canNotMerge(handPiece)) ||
                    piece is Castle ||
                    piece is Capital)
              })
      attackableHexes.addAll(
          territory.enemyBorderHexes.filter { territory.island.canAttack(it, handPiece) })

      val hexagon = attackableHexes.randomOrNull() ?: return
      gameInputProcessor.click(hexagon)
    }
  }

  @ExperimentalStdlibApi
  override fun action(island: Island, gameInputProcessor: GameInputProcessor) {
    island.select(island.hexagons.first())
    for (territory in island.getTerritories(team)) {
      do {
        island.select(territory.hexagons.first())
        val sel = territory.island.selected ?: continue

        if (!pickUp(sel, gameInputProcessor)) {
          break
        }
        place(sel, gameInputProcessor)
      } while (random.nextFloat() > 0.005f)
    }
    island.select(null)
  }
}
