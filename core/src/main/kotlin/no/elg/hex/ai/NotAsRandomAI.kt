package no.elg.hex.ai

import com.badlogic.gdx.Gdx
import kotlinx.coroutines.yield
import no.elg.hex.hexagon.BARON_STRENGTH
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
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.getTerritories
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon
import java.util.Arrays
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.random.Random.Default as random

/**
 * An AI that does semi-random actions for a random amount of time. All action it can take will
 * have some value. For example, it will not click on an empty hexagon in a territory when it does
 * not hold anything in its hand.
 *
 * This AI operates independently on each territory in the given team. It has no long term goals.
 *
 * @author Elg
 */
class NotAsRandomAI(
  /**
   * The team of this AI
   */
  override val team: Team,
  /**
   * How many turns must elapse before the AI is allowed to buy castles.
   *
   * That is, a lower value will make this a harder AI
   */
  private val castleDelay: Int,
  /**
   * How likely it is that the AI will end it's turn.
   * A value of `0` means it will never end its turn before it has tried to pick up all it's pieces
   * while a value of `1` means it will only try a single action before ending it's turn
   *
   * That is, a lower value will make this a harder AI
   */
  private val endTurnChance: Float
) : AI {

  private fun think(words: () -> String) {
    Gdx.app.trace("NARAI-$team", message = words)
  }

  /**
   * list of hexagon not to be picked up, as no action could be done with it, clear every round
   */
  private val hexBlacklist = ArrayList<Hexagon<HexagonData>>()
  private val hexPickupPritoryList = ArrayList<Hexagon<HexagonData>>()
  private val strengthUsable = Array(BARON_STRENGTH) { true }

  private fun resetBlacklists() {
    hexBlacklist.clear()
    hexPickupPritoryList.clear()
    Arrays.fill(strengthUsable, true)
  }

  private fun canUseStrength(str: Int): Boolean = strengthUsable[str - 1]
  private fun cannotUseStrength(str: Int) {
    strengthUsable[str - 1] = false
  }

  private val buyablePieces =
    arrayOf(
      Castle::class,
      Peasant::class,
      Peasant::class
//      Spearman::class,
//      Knight::class,
//      Baron::class,
    )

  override suspend fun action(island: Island, gameInputProcessor: GameInputProcessor): Boolean {
    island.select(island.visibleHexagons.first())
    val territories = island.getTerritories(team)
    for (territory in territories) {
      resetBlacklists()
      do {
        yield()
        island.select(territory.hexagons.first())
        val sel = territory.island.selected ?: continue

        if (!pickUp(sel, gameInputProcessor)) {
          break
        }
        place(sel, gameInputProcessor)
      } while (random.nextFloat() > endTurnChance)
    }
    island.select(null)
    resetBlacklists()
    return territories.isNotEmpty()
  }

  private fun pickUp(territory: Territory, gameInputProcessor: GameInputProcessor): Boolean {
    think { "Picking up a piece" }
    if (territory.island.hand?.piece != null) {
      think { "Already holding a piece! (${territory.island.hand?.piece})" }
      return true
    }

    if (hexPickupPritoryList.isNotEmpty()) {
      val hex = hexPickupPritoryList.removeLast()
      think {
        val data = territory.island.getData(hex)
        "Picking up priority piece ${data.piece} at ${hex.cubeCoordinate.toAxialKey()}"
      }
      gameInputProcessor.click(hex)
      return true
    }

    // hexagons we can pick up pieces from
    val pickUpHexes =
      HashSet<Hexagon<HexagonData>>(
        territory.hexagons.filter {
          val piece = territory.island.getData(it).piece
          piece is LivingPiece && !piece.moved && it !in hexBlacklist && canUseStrength(piece.strength)
        }
      )

    // only buy if there are no more units to move
    val isHolding = if (pickUpHexes.isNotEmpty()) {
      think { "There is something to pick up in the current territory!" }
      gameInputProcessor.click(pickUpHexes.random())
      true
    } else {
      buy(territory, gameInputProcessor)
    }
    think { "I am now holding ${territory.island.hand?.piece}" }
    return isHolding
  }

  private fun buy(territory: Territory, gameInputProcessor: GameInputProcessor): Boolean {
    think { "No pieces to pick up. Buying a unit (balance ${territory.capital.balance})" }

    val pieceToBuy = kotlin.run {
      if (allowedToBuyCastle(territory) && shouldBuyCastle() && existsEmptyHexagon(territory) && territory.capital.canBuy(Castle::class)) {
        // No upkeep for castles so as long as there is an empty hexagon, we can buy a castle
        return@run Castle::class.createHandInstance()
      } else {
        for (living in LivingPiece::class.sealedSubclasses.map { it.createHandInstance() }.sortedBy { it.strength }) {
          if (isEconomicalToBuyPiece(territory, living) && canAttackOrMergePiece(territory, living)) {
            // If we can do something with a lower strength piece, then it is preferred
            return@run living
          }
        }
        // We cannot buy any piece!
        return@run null
      }
    }
    if (pieceToBuy == null) {
      think { "No pieces to pickup, and cannot afford any pieces in this territory, I only have ${territory.capital.balance} and an income of ${territory.income}" }
      return false
    }

    think { "Buying the unit ${pieceToBuy::class.simpleName} " }
    gameInputProcessor.buyUnit(pieceToBuy)
    think { "New balance ${territory.capital.balance}" }
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

      val hexagon = kotlin.run {
        if (treeHexagons.isNotEmpty()) {
          think { "There is a piece of tree in my territory, must tear it down!" }
          return@run treeHexagons.random()
        }

        val attackableHexes = attackableHexagons(territory, handPiece)
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

          return@run connectingHex
            ?: tryAttack(Capital::class)
            ?: tryAttack(Castle::class)
            ?: tryAttack(LivingPiece::class)
            // Take over territory which is well defended, also helps with mass attacks
            ?: attackableHexes.maxByOrNull { island.calculateStrength(it, territory.team) }
            ?: kotlin.run {
              think { "Failed to attack anything" }
              return
            }
        } else {
          think { "No enemy territory is attackable" }

          val graveHexagons = territory.hexagons.filter { island.getData(it).piece is Grave }
          if (graveHexagons.isNotEmpty()) {
            think { "But there is a hexagon in my territory with a grave, lets clean it up early" }
            return@run graveHexagons.random()
          }

          val mergeHex = bestPieceToMergeWith(territory, handPiece)
          if (mergeHex != null) {
            think { "Merging hand with ${territory.island.getData(mergeHex).piece}" }
            resetBlacklists()
            hexPickupPritoryList += mergeHex
            return@run mergeHex
          } else {
            think { "Found no acceptable pieces to merge held item with" }
          }

          cannotUseStrength(handPiece.strength)
          think { "Placing the held piece in the best defensive position, nothing else to do" }
          val emptyHex = calculateBestLivingDefencePosition(territory)
          if (emptyHex != null) {
            hexBlacklist += emptyHex
          } else {
            think { "Failed to place held piece on any hexagons" }
            return
          }
          return@run emptyHex
        }
      }
      think {
        "Placing piece $handPiece at ${hexagon.cubeCoordinate.toAxialKey()} " +
          "(which is a ${island.getData(hexagon).piece} of team ${island.getData(hexagon).team})"
      }
      gameInputProcessor.click(hexagon)
    }
  }

  /* *************************************************************************
   *  UTILITY METHODS
   * *************************************************************************/

  private fun allowedToBuyCastle(territory: Territory): Boolean {
    return territory.island.round > castleDelay
  }

  private fun shouldBuyCastle(): Boolean {
    return Random.nextDouble() > BUY_CASTLE_CHANCE
  }

  private fun bestPieceToMergeWith(
    territory: Territory,
    handPiece: LivingPiece
  ): Hexagon<HexagonData>? {
    think { "Will try to merge piece with another piece" }
    val maxBorderStr =
      territory.enemyTerritoryHexagons.maxOfOrNull { territory.island.getData(it).piece.strength }
        ?: NO_STRENGTH

    think {
      if (maxBorderStr > 0) {
        "The highest level threat on my border has the strength of a ${strengthToType(maxBorderStr).simpleName}"
      } else {
        "I have vanquished my foes!"
      }
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
      val canMergedAttackAnything = canPieceAttack(territory, mergedType)
      val canAttack = !piece.moved && !handPiece.moved && canMergedAttackAnything

      // If we have not yet placed the held piece (ie directly merging it) we do not pay upkeep on it
      val handUpkeep = if (handPiece.data == HexagonData.EDGE_DATA) 0 else handPiece.income

      val newIncome = territory.income - piece.income - handUpkeep + mergedType.income

      think {
        "Checking if I should merge ${handPiece::class.simpleName} with ${piece::class.simpleName}. " +
          "The merged piece can${if (canAttack) "" else " not"} be used to attack a bordering territory."
      }
      val shouldCreate = canAttack && isEconomicalToCreatePiece(territory.capital.balance, newIncome, true)
      think {
        "New income would be $newIncome, current income is ${territory.income}, " +
          "current balance is ${territory.capital.balance}, this is " +
          if (shouldCreate) "acceptable" else "not acceptable, trying again"
      }
      return@find shouldCreate
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

          island.calculateStrength(it) + (neighborStrength.sum().toDouble() / neighborStrength.size)
        }

    // find any hexagon with that is protected the least
    val minStr = placeableHexes.values.minOrNull() ?: return null

    val leastDefendedHexes =
      placeableHexes.filter { (hex, str) ->
        str <= minStr &&
          // Never place a castle next to another castle
          island.getNeighbors(hex).map { island.getData(it) }.filter { it.team == island.getData(hex).team }
            .none { it.piece is Castle }
      }.mapTo(ArrayList()) { it.key }

    // shuffle the list to make the selection more uniform
    leastDefendedHexes.shuffle()

    // there are multiple hexagons that are defended as badly, choose the hexagon that will protect
    // the most hexagons
    return leastDefendedHexes.maxByOrNull {
      // note that this will give a slight disadvantage to hexagons surrounded by sea, as we look at
      // the absolute number of neighbors
      val neighbors = island.getNeighbors(it)
      neighbors.count { neighbor -> island.getData(neighbor).team == team } / neighbors.size
    }
  }

  private fun calculateBestLivingDefencePosition(territory: Territory): Hexagon<HexagonData>? {
    val island = territory.island
    val team = territory.team
    val placeableHexes =
      territory.hexagons
        .filter {
          // all legal hexagons we can place a piece at
          val piece = island.getData(it).piece
          piece is Empty || piece is TreePiece || piece is Grave
        }
        .associateWith { hex ->
          val neighbors = island.getNeighbors(hex)
          val strength = island.calculateStrength(hex).toDouble()

          // we don't want to be placed on the border, but if we must then do it where there is the least defence
          if (neighbors.any { island.getData(it).team != team }) {
            return@associateWith -strength
          }

          val neighborStrength = neighbors.map { neighbor ->

            // how much the strength an enemy neighbor has
            island.getNeighbors(neighbor)
              .filter { island.getData(it).team != team }
              .maxOfOrNull { island.calculateStrength(it) }
              ?: 0
          }

          val normalizedNeighborStrength = neighborStrength.sum().toDouble() / neighborStrength.size
          // subtract current strength of a hex to not overprotect a given hex
          return@associateWith normalizedNeighborStrength - strength
        }

    // find any hexagon with that is protected the least
    val maxStr = placeableHexes.values.maxOrNull() ?: return null

    val leastDefendedHexes =
      placeableHexes.filter { (_, str) -> str >= maxStr }.mapTo(ArrayList()) { it.key }

    // shuffle the list to make the selection more uniform
    leastDefendedHexes.shuffle()

    // there are multiple hexagons that are defended as badly, choose the hexagon that will protect
    // the most hexagons
    return leastDefendedHexes.maxByOrNull {
      val neighbors = island.getNeighbors(it)
      neighbors.count { neighbor -> island.getData(neighbor).team == team } / neighbors.size
    }
  }

  private fun attackableHexagons(territory: Territory, piece: LivingPiece): List<Hexagon<HexagonData>> {
    return territory.enemyBorderHexes.filter { territory.island.canAttack(it, piece) } +
      territory.hexagons.filter { hex -> territory.island.getData(hex).piece is TreePiece }
  }

  private fun canPieceAttack(territory: Territory, piece: LivingPiece): Boolean {
    return territory.enemyBorderHexes.any { hexagon -> territory.island.canAttack(hexagon, piece) } ||
      territory.hexagons.any { hex -> territory.island.getData(hex).piece is TreePiece }
  }

  private fun canAttackOrMergePiece(territory: Territory, piece: LivingPiece): Boolean {
    if (canPieceAttack(territory, piece)) {
      return true
    }
    val mergeWith = bestPieceToMergeWith(territory, piece) ?: return false
    val mergePiece = territory.island.getData(mergeWith).piece
    require(mergePiece is LivingPiece) { "Merge piece is not a living piece" }
    val mergedType = mergedType(piece, mergePiece).createHandInstance()
    return canPieceAttack(territory, mergedType)
  }

  private fun existsEmptyHexagon(territory: Territory): Boolean {
    return territory.hexagons.any {
      territory.island.getData(it).piece is Empty
    }
  }

  private fun isEconomicalToBuyPiece(territory: Territory, piece: LivingPiece): Boolean {
    // only buy pieces we can maintain for at least PIECE_MAINTAIN_CONTRACT_LENGTH turns
    val newBalance = territory.capital.balance - piece.price
    val newIncome = territory.income + piece.income
    return territory.capital.canBuy(piece) && isEconomicalToCreatePiece(
      newBalance,
      newIncome,
      canPieceAttack(territory, piece)
    )
  }

  companion object {
    const val BUY_CASTLE_CHANCE = 1.0 / 10.0

    const val MAX_TOTAL_BARONS = 5
    const val MAX_TOTAL_KNIGHTS = 15

    /**
     * Minimum number of turns we should aim to maintain a piece before bankruptcy.
     * A higher number means lower risk of a bankruptcy.
     *
     * * A value of 0 means we might go bankrupt before beginning next turn
     * * A value of 1 makes sure we will survive at least till the next turn
     *
     * @see isEconomicalToCreatePiece
     */
    const val PIECE_MAINTAIN_CONTRACT_LENGTH = 2

    /**
     * Minimum balance projected to have next turn.
     * A lower value allows for more risky investment, but runs a higher risk of bankruptcy.
     *
     * With a value less than zero we assume we can place piece to not go bankrupt.
     *
     * @see isEconomicalToCreatePiece
     */
    private const val MINIMUM_NEXT_TURN_INCOME_ATTACKABLE_PIECE = -1

    /**
     * If we should buy/merge pieces with the given [currentBalance] and [projectedIncome]
     *
     * @see PIECE_MAINTAIN_CONTRACT_LENGTH
     */
    fun isEconomicalToCreatePiece(currentBalance: Int, projectedIncome: Int, canAttack: Boolean = false): Boolean {
      if (projectedIncome >= 0) return true
      val minIncome = if (canAttack) MINIMUM_NEXT_TURN_INCOME_ATTACKABLE_PIECE else 0
      return currentBalance + projectedIncome * PIECE_MAINTAIN_CONTRACT_LENGTH >= minIncome
    }
  }
}