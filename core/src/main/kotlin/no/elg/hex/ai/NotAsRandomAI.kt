package no.elg.hex.ai

import com.badlogic.gdx.Gdx
import java.util.Collections.shuffle
import kotlin.random.Random.Default as random
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Team
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.canAttack
import no.elg.hex.util.createInstance
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
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
class NotAsRandomAI(override val team: Team) : AI {

  private val buyablePieces =
      arrayOf(Castle::class
          //         , Peasant::class,
          //          Spearman::class,
          //          Knight::class,
          //          Baron::class,
          //          Peasant::class,
          //          Spearman::class
          )

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
      val hexagon = calculateBestCastlePlacement(territory) ?: return
      gameInputProcessor.click(hexagon)
    } else if (handPiece is LivingPiece) {

      // hexagon where we can put a living piece
      val attackableHexes =
          HashSet<Hexagon<HexagonData>>(
              territory.hexagons.filter {
                val piece = territory.island.getData(it).piece
                !((piece is LivingPiece && !piece.canMerge(handPiece)) ||
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

  fun calculateBestCastlePlacement(territory: Territory): Hexagon<HexagonData>? {
    val island = territory.island
    val placeableHexes =
        territory.hexagons
            .filter {
              // all legal hexagons we can place a castle at (which is all empty hexes)
              island.getData(it).piece is Empty
            }
            .associateWith {
              val neighborStrength =
                  island.getNeighbors(it).map { neighbor -> island.calculateStrength(neighbor) }

              island.calculateStrength(it) +
                  (neighborStrength.sum().toDouble() / neighborStrength.size)
            }
    if (placeableHexes.size <= 1) {
      return placeableHexes.keys.firstOrNull()
    }

    // find any hexagon with that is protected the least
    val minStr = placeableHexes.values.min() ?: return null

    val leastDefendedHexes = placeableHexes.filter { (_, str) -> str <= minStr }.map { it.key }
    if (leastDefendedHexes.size <= 1) {
      return leastDefendedHexes.firstOrNull()
    }

    // there are multiple hexagons that are defended as badly, choose the hexagon that will protect
    // the most hexagons

    return leastDefendedHexes
        .mapTo(ArrayList()) {
          it to
              island
                  .getNeighbors(it)
                  .filter { neighbor -> island.getData(neighbor).team == team }
                  .count()
        }
        .apply { shuffle() }
        .maxBy { it.second }
        ?.first
  }
}
