package no.elg.hex.ai

import com.badlogic.gdx.Gdx
import kotlin.math.abs
import kotlin.random.Random.Default as random
import kotlin.reflect.KClass
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Grave
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.KNIGHT_STRENGTH
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.NO_STRENGTH
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.SPEARMAN_STRENGTH
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.TreePiece
import no.elg.hex.hexagon.mergedType
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.canAttack
import no.elg.hex.util.createHandInstance
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
class NotAsRandomAI(override val team: Team, val printActions: Boolean = false) : AI {

  private fun think(words: () -> String) {
    Gdx.app.trace("NARAI-$team", words)
  }

  private val buyablePieces =
      arrayOf(Castle::class, Peasant::class
          //          Spearman::class,
          //          Knight::class,
          //          Baron::class,
          //          Peasant::class,
          //          Spearman::class
          )

  fun pickUp(territory: Territory, gameInputProcessor: GameInputProcessor): Boolean {
    think { "Picking up a piece" }
    if (territory.island.inHand?.piece != null) {
      think { "Already holding a piece! (${territory.island.inHand?.piece})" }
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
          "NARAI-$team",
          "No pieces to pick up. Buying a unit (balance ${territory.capital.balance})")
      val piece = buyablePieces.filter { territory.capital.canBuy(it) }.randomOrNull()
      if (piece == null) {
        Gdx.app.trace(
            "NARAI-$team",
            "Cannot afford any pieces in this territory, I only have ${territory.capital.balance}")
        return false
      }
      think { "Buying the unit ${piece.simpleName} " }
      gameInputProcessor.buyUnit(piece.createInstance(HexagonData.EDGE_DATA))
      think { "New balance ${territory.capital.balance}" }
    } else {
      think { "There is something to pick up in the current territory!" }
      gameInputProcessor.click(pickUpHexes.random())
    }
    think { "I am now holding ${territory.island.inHand?.piece}" }
    return true
  }

  fun place(territory: Territory, gameInputProcessor: GameInputProcessor) {

    val handPiece = territory.island.inHand?.piece
    think { "Placing held piece $handPiece" }
    if (handPiece == null) {
      think { "Not holding any piece!" }
      return
    }
    if (handPiece is Castle) {
      // hexagons where we can place castles
      val hexagon = calculateBestCastlePlacement(territory)
      if (hexagon == null) {
        think { "No valid hexagon to place the castle!" }
        return
      }
      think { "Best placement for this castle is ${hexagon.cubeCoordinate.toAxialKey()}" }
      gameInputProcessor.click(hexagon)
    } else if (handPiece is LivingPiece) {

      val treeHexagons =
          territory.hexagons.filter { territory.island.getData(it).piece is TreePiece }

      val hexagon =
          if (treeHexagons.isNotEmpty()) {
            think { "There is a piece of tree in my territory, must tear it down!" }
            treeHexagons.random()
          } else {
            // hexagon where we can put a living piece
            //        val attackableHexes =
            //            HashSet<Hexagon<HexagonData>>(
            //                territory.hexagons.filter {
            //                  val piece = territory.island.getData(it).piece
            //                  ((piece is LivingPiece && piece.canMerge(handPiece)) ||
            //                      piece is Castle ||
            //                      piece is Capital)
            //                })

            val attackableHexes =
                territory.enemyBorderHexes.filter { territory.island.canAttack(it, handPiece) }
            if (attackableHexes.isNotEmpty()) {
              think { "I can attack an enemy hexagon with this piece" }

              fun tryAttack(type: KClass<out Piece>): Hexagon<HexagonData>? {
                val attackableOfType =
                    attackableHexes.filter { type.isInstance(territory.island.getData(it).piece) }
                if (attackableOfType.isNotEmpty()) {
                  think { "Will attack a ${type.simpleName}" }
                  attackableOfType.random()
                }
                return null
              }

              tryAttack(Capital::class)
                  ?: tryAttack(Castle::class) ?: tryAttack(LivingPiece::class)
                      ?: attackableHexes.randomOrNull() ?: return
            } else {
              think { "No territory is attackable" }

              val graveHexagons =
                  territory.hexagons.filter { territory.island.getData(it).piece is Grave }
              if (graveHexagons.isNotEmpty()) {
                think { "But there is a hexagon with a grave, lets clean it up early" }
                graveHexagons.random()
              } else if (random.nextFloat() >= 0.25) {
                think { "Place the held piece randomly" }
                territory
                    .hexagons
                    .filter { territory.island.getData(it).piece is Empty }
                    .randomOrNull()
              } else {
                val maxBorderStr =
                    territory
                        .enemyBorderHexes
                        .map { territory.island.getData(it).piece.strength }
                        .max()
                        ?: NO_STRENGTH

                val shouldMergeToKnight =
                    maxBorderStr >= SPEARMAN_STRENGTH &&
                        territory.hexagons.count { territory.island.getData(it).piece is Knight } <
                            MAX_TOTAL_KNIGHTS - 1
                val shouldMergeToBaron =
                    maxBorderStr >= KNIGHT_STRENGTH &&
                        territory.hexagons.count {
                          val piece = territory.island.getData(it).piece
                          piece is Knight || piece is Baron
                        } < MAX_TOTAL_BARONS - 1
                if (!shouldMergeToBaron) {
                  think { "Should not merge to baron right now" }
                }
                if (!shouldMergeToKnight) {
                  think { "Should not merge to knight right now" }
                }

                territory.hexagons.find {
                  val piece = territory.island.getData(it).piece
                  if (piece !is LivingPiece || piece.canNotMerge(handPiece)) {
                    return@find false
                  }
                  if (piece is Knight && !shouldMergeToKnight) {
                    return@find false
                  }
                  if (piece is Baron && !shouldMergeToBaron) {
                    return@find false
                  }

                  val mergedType = mergedType(piece, handPiece).createHandInstance()

                  val newIncome = territory.income - piece.cost - handPiece.cost + mergedType.cost
                  think {
                    "NewIncome would be $newIncome I want at least ${mergedType.cost / 2} to accept this merge"
                  }
                  newIncome >= abs(mergedType.cost) / 2
                }
              }
            }
          }
      if (hexagon == null) {
        think { "Failed to find any enemy hexagon to attack" }
        return
      }
      think {
        "Placing piece $handPiece at ${hexagon.cubeCoordinate.toAxialKey()} (which is a ${territory.island.getData(hexagon).piece} of team ${territory.island.getData(hexagon).team})"
      }
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

    // find any hexagon with that is protected the least
    val minStr = placeableHexes.values.min() ?: return null

    val leastDefendedHexes =
        placeableHexes.filter { (_, str) -> str <= minStr }.mapTo(ArrayList()) { it.key }

    // shuffle the list to make the selection more uniform
    leastDefendedHexes.shuffle()

    // there are multiple hexagons that are defended as badly, choose the hexagon that will protect
    // the most hexagons
    return leastDefendedHexes.maxBy {
      // note that this will give a slight disadvantage to hexagons surrounded by sea, as we look at
      // the absolute number of neighbors
      island.getNeighbors(it).filter { neighbor -> island.getData(neighbor).team == team }.count()
    }
  }

  companion object {
    const val MAX_TOTAL_BARONS = 1
    const val MAX_TOTAL_KNIGHTS = 3
  }
}