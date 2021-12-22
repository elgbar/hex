package no.elg.hex.ai

import com.badlogic.gdx.Gdx
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
import no.elg.hex.hexagon.strengthToType
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
import kotlin.reflect.KClass
import kotlin.random.Random.Default as random

/**
 * An AI that does semi-random actions for a until it amount of time. All action it can take will
 * have some value. For example it will not click on an empty hexagon in a territory when it does
 * not hold anything in it's hand.
 *
 * This AI operates independently on each territory in the given team. It has no long term goals.
 *
 * @author Elg
 */
class NotAsRandomAI(override val team: Team) : AI {

  private fun think(words: () -> String) {
    Gdx.app.trace("NARAI-$team", words)
  }

  /**
   * list of hexagon not to be picked up, as no action could be done with it, clear every round
   */
  private val hexBlacklist = ArrayList<Hexagon<HexagonData>>()

  private val buyablePieces =
    arrayOf(
      Castle::class,
      Peasant::class,
      Peasant::class,
//      Spearman::class,
//      Knight::class,
//      Baron::class,
    )

  override fun action(island: Island, gameInputProcessor: GameInputProcessor) {
    island.select(island.hexagons.first())
    for (territory in island.getTerritories(team)) {
      hexBlacklist.clear()
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

  private fun pickUp(territory: Territory, gameInputProcessor: GameInputProcessor): Boolean {
    think { "Picking up a piece" }
    if (territory.island.hand?.piece != null) {
      think { "Already holding a piece! (${territory.island.hand?.piece})" }
      return true
    }

    // hexagons we can pick up pieces from
    val pickUpHexes =
      HashSet<Hexagon<HexagonData>>(
        territory.hexagons.filter {
          val piece = territory.island.getData(it).piece
          piece is LivingPiece && !piece.moved && !hexBlacklist.contains(it)
        }
      )

    // only buy if there are no more units to move
    if (pickUpHexes.isEmpty()) {
      think {
        "No pieces to pick up. Buying a unit (balance ${territory.capital.balance})"
      }
      val piece = buyablePieces.filter {
        val toBuy = it.createHandInstance()
        // only buy pieces we can maintain for at least PIECE_MAINTAIN_CONTRACT_LENGTH turns
        val newBalance = territory.capital.balance - toBuy.price
        val newIncome = territory.income + toBuy.income

        newBalance > 0 && shouldCreate(newBalance, newIncome)
      }.randomOrNull()

      if (piece == null) {
        think { "Cannot afford any pieces in this territory, I only have ${territory.capital.balance} and an income of ${territory.income}" }
        return false
      }
      think { "Buying the unit ${piece.simpleName} " }
      gameInputProcessor.buyUnit(piece.createInstance(HexagonData.EDGE_DATA))
      think { "New balance ${territory.capital.balance}" }
    } else {
      think { "There is something to pick up in the current territory!" }
      gameInputProcessor.click(pickUpHexes.random())
    }
    think { "I am now holding ${territory.island.hand?.piece}" }
    return true
  }

  private fun place(territory: Territory, gameInputProcessor: GameInputProcessor) {

    val island = territory.island
    val handPiece = island.hand?.piece
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
        territory.hexagons.filter { island.getData(it).piece is TreePiece }

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
            territory.enemyBorderHexes.filter { island.canAttack(it, handPiece) }
          if (attackableHexes.isNotEmpty()) {
            think { "I can attack an enemy hexagon with this piece" }

            fun tryAttack(type: KClass<out Piece>): Hexagon<HexagonData>? {
              val attackableOfType =
                attackableHexes.filter { type.isInstance(island.getData(it).piece) }
              if (attackableOfType.isNotEmpty()) {
                think { "Will attack a ${type.simpleName}" }
                return attackableOfType.random()
              }
              think { "Cannot find any ${type.simpleName} to attack" }
              return null
            }

            val connectingHex = attackableHexes.firstOrNull {
              island.getNeighbors(it).filter { fit ->
                fit !in territory.hexagons
              }.any { ait -> island.getData(ait).team == territory.team }
            }
            think { "Is there a nearby friendly territory? ${connectingHex != null} (${connectingHex?.cubeCoordinate})" }

            connectingHex ?: tryAttack(Capital::class)
              ?: tryAttack(Castle::class)
              ?: tryAttack(LivingPiece::class)
              // Take over territory which is well defended, also helps with mass attacks
              ?: attackableHexes.maxByOrNull { island.calculateStrength(it, territory.team) }
              ?: return
          } else {
            think { "No enemy territory is attackable" }

            val graveHexagons =
              territory.hexagons.filter { island.getData(it).piece is Grave }
            when {
              graveHexagons.isNotEmpty() -> {
                think { "But there is a hexagon with a grave, lets clean it up early" }
                graveHexagons.random()
              }
              random.nextFloat() >= 0.15f -> { // 85% chance of placing defensively
                think { "Placing the held piece in the best defensive position, nothing else to do" }
                val emptyHex = calculateBestCastlePlacement(territory)
                if (emptyHex != null) hexBlacklist.add(emptyHex)
                emptyHex
              }
              else -> {
                mergeWithLivingTerritoryPiece(handPiece, territory)
              }
            }
          }
        }
      if (hexagon == null) {
        think { "Failed to find any enemy hexagon to attack" }
        return
      }
      think {
        "Placing piece $handPiece at ${hexagon.cubeCoordinate.toAxialKey()} " +
          "(which is a ${territory.island.getData(hexagon).piece} of team ${territory.island.getData(hexagon).team})"
      }
      gameInputProcessor.click(hexagon)
    }
  }

  private fun mergeWithLivingTerritoryPiece(
    handPiece: LivingPiece,
    territory: Territory
  ): Hexagon<HexagonData>? {
    think { "Will try to merge piece with another piece" }
    val maxBorderStr =
      territory.enemyBorderHexes.maxOfOrNull { territory.island.getData(it).piece.strength }
        ?: NO_STRENGTH

    think {
      if (maxBorderStr > 0) "The highest level threat on my border has the strength of a ${strengthToType(maxBorderStr).simpleName}"
      else "I have vanquished my foes!"
    }

    val knights = territory.hexagons.count { territory.island.getData(it).piece is Knight }
    val barons = territory.hexagons.count { territory.island.getData(it).piece is Baron }

    think { "There are $knights knights and $barons barons in this territory" }

    val disallowKnight = maxBorderStr < SPEARMAN_STRENGTH || knights >= MAX_TOTAL_KNIGHTS - 1
    if (disallowKnight) {
      think { "Should not merge to knight right now as the threat is not high enough" }
    }

    val disallowBaron = maxBorderStr < KNIGHT_STRENGTH || barons >= MAX_TOTAL_BARONS - 1
    if (disallowBaron) {
      think { "Should not merge to baron right now as the threat is not high enough" }
    }

    think { "Trying to find piece to merge held ${handPiece::class.simpleName} with" }

    val found = territory.hexagons.find {
      val piece = territory.island.getData(it).piece
      if (piece !is LivingPiece || piece.canNotMerge(handPiece)) {
        return@find false
      }
      if (piece is Knight && disallowKnight) {
        return@find false
      }
      if (piece is Baron && disallowBaron) {
        return@find false
      }

      val mergedType = mergedType(piece, handPiece).createHandInstance()

      think { "Checking if I should merge ${handPiece::class.simpleName} with ${piece::class.simpleName}" }

      // If we have not yet placed the held piece (ie directly merging it) we do not pay upkeep on it
      val handUpkeep = if (handPiece.data == HexagonData.EDGE_DATA) 0 else handPiece.income

      val newIncome = territory.income - piece.income - handUpkeep + mergedType.income
      val shouldCreate = shouldCreate(territory.capital.balance, newIncome)
      think { "New income would be $newIncome, current income is ${territory.income}, current balance is ${territory.capital.balance}, this is ${if (shouldCreate) "acceptable" else "not acceptable, trying again"}" }
      return@find shouldCreate
    }
    if(found == null){
      think { "Found no acceptable pieces to merge held item with" }
    }else{
      // clear blacklist as a higher tier of units might mean some hexagon will now be attackable
      hexBlacklist.clear()
      think { "Merging hand with ${territory.island.getData(found).piece}" }
    }
    return found
  }

  private fun calculateBestCastlePlacement(territory: Territory): Hexagon<HexagonData>? {
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
    val minStr = placeableHexes.values.minOrNull() ?: return null

    val leastDefendedHexes =
      placeableHexes.filter { (_, str) -> str <= minStr }.mapTo(ArrayList()) { it.key }

    // shuffle the list to make the selection more uniform
    leastDefendedHexes.shuffle()

    // there are multiple hexagons that are defended as badly, choose the hexagon that will protect
    // the most hexagons
    return leastDefendedHexes.maxByOrNull {
      // note that this will give a slight disadvantage to hexagons surrounded by sea, as we look at
      // the absolute number of neighbors
      island.getNeighbors(it).count { neighbor -> island.getData(neighbor).team == team }
    }
  }

  companion object {
    const val MAX_TOTAL_BARONS = 1
    const val MAX_TOTAL_KNIGHTS = 3

    /**
     * Minimum number of turns we should aim to maintain a piece before bankruptcy.
     * A higher number means higher risk of a bankruptcy.
     *
     * * A value of 0 means we might go bankrupt before beginning next turn
     * * A value of 1 makes sure we will survive at least till the next turn
     *
     * @see shouldCreate
     */
    const val PIECE_MAINTAIN_CONTRACT_LENGTH = 2

    /**
     * If we should buy/merge pieces with the given [currentBalance] and [projectedIncome]
     *
     * @see PIECE_MAINTAIN_CONTRACT_LENGTH
     */
    fun shouldCreate(currentBalance: Int, projectedIncome: Int): Boolean {
      if(projectedIncome >= 0) return true
      return currentBalance + projectedIncome * PIECE_MAINTAIN_CONTRACT_LENGTH >= 0
    }
  }
}
