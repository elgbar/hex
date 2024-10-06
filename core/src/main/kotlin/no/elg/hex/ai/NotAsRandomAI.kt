package no.elg.hex.ai

import com.badlogic.gdx.Gdx
import kotlinx.coroutines.yield
import no.elg.hex.Hex
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
import no.elg.hex.hexagon.Spearman
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.TreePiece
import no.elg.hex.hexagon.mergedType
import no.elg.hex.hexagon.strengthToType
import no.elg.hex.input.GameInteraction
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.canAttack
import no.elg.hex.util.coordinates
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.filterIsPiece
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.info
import no.elg.hex.util.isPartOfATerritory
import no.elg.hex.util.isPiece
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
   * How likely it is that the AI will end its turn.
   * A value of `0` means it will never end its turn before it has tried to pick up all it's pieces
   * while a value of `1` means it will only try a single action before ending its turn
   *
   * That is, a lower value will make this a harder AI
   */
  private val endTurnChance: Double,
  /**
   * How likely it is that the AI will buy a castle given it is allowed to buy castles
   *
   * Value should be between `0` and `1`
   */
  private val buyCastleChance: Double
) : AI {

  private val tag = "NARAI-$team"

  private fun think(territory: Territory?, words: () -> String) {
    if (Hex.args.`ai-debug`) {
      Gdx.app.info(tag + territory?.capitalHexagon?.coordinates, message = words)
    } else {
      Gdx.app.trace(tag + territory?.capitalHexagon?.coordinates, message = words)
    }
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

  override suspend fun action(island: Island, gameInteraction: GameInteraction) {
    val capitals = island.filterIsPiece<Capital>(team)
    for (capitalHexagon in capitals) {
      if (!capitalHexagon.isPiece<Capital>(island)) {
        think(null) { "Next capital is not a capital anymore, it must have been merged with another territory" }
        continue
      }

      do {
        yield()
        island.hand?.also { hand ->
          think(hand.territory) { "uh-oh when starting an action I am still holding $hand" }
          island.select(null) // make sure we don't have any selected hexagons when going for the next hexagon
        }

        // Select the territory by selecting the capital
        // By selecting the capital we are use we don't pick up any piece in the territory
        island.select(capitalHexagon)
        val territory = island.selected
        if(territory == null) {
          think(null) { "Could not select territory for capital at ${capitalHexagon.coordinates}" }
          break
        }

        if (!territory.checkWeaklyValid()) {
          think(territory) { "This territory is not longer weakly valid, skipping to next territory" }
          break
        }

        if (!pickUpOrBuy(territory, gameInteraction)) {
          think(territory) { "No piece to buy or pick up, skipping to next territory" }
          break
        }
        place(territory, gameInteraction)
      } while (random.nextFloat() > endTurnChance)
    }
    think(null) { "Ending turn" }
    island.select(null)
    resetBlacklists()
  }

  private fun pickUpOrBuy(territory: Territory, gameInteraction: GameInteraction): Boolean {
    val island = territory.island
    think(territory) { "Picking up a piece" }
    if (island.hand?.piece != null) {
      think(territory) { "Already holding a piece! (${island.hand?.piece})" }
      return true
    }

    if (hexPickupPritoryList.isNotEmpty()) {
      @Suppress("NewApi")
      val hex = hexPickupPritoryList.removeLast()
      think(territory) {
        val data = island.getData(hex)
        "Picking up priority piece ${data.piece} at ${hex.coordinates}"
      }
      gameInteraction.click(hex)
      return true
    }

    // hexagons we can pick up pieces from
    val pickUpHexes = territory.hexagons.filter {
      val piece = island.getData(it).piece
      piece is LivingPiece && !piece.moved && it !in hexBlacklist && canUseStrength(piece.strength)
    }.toSet()

    // only buy if there are no more units to move
    val isHolding = if (pickUpHexes.isNotEmpty()) {
      think(territory) { "There is something to pick up in the current territory!" }
      gameInteraction.click(pickUpHexes.random())
      true
    } else {
      think(territory) { "No pieces to pick up. Buying a unit (balance ${territory.capital.balance})" }
      buy(territory, gameInteraction)
    }
    think(territory) { "I am now holding ${island.hand?.piece}" }
    return isHolding
  }

  private fun buy(territory: Territory, gameInteraction: GameInteraction): Boolean {
    val pieceToBuy = run {
      if (allowedToBuyCastle(territory.island.round) && shouldBuyCastle() && territory.capital.canBuy<Castle>() && existsEmptyHexagon(territory)) {
        // No upkeep for castles so as long as there is an empty hexagon, we can buy a castle
        return@run Castle::class.createHandInstance()
      } else {
        for (livingClass in LivingPiece.subclassedSortedByStrength) {
          val living = livingClass.createHandInstance()
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
      think(territory) { "No pieces to pickup, and cannot afford any pieces in this territory, I only have ${territory.capital.balance} and an income of ${territory.income}" }
      return false
    }

    think(territory) { "Buying the unit ${pieceToBuy::class.simpleName} " }
    gameInteraction.buyUnit(pieceToBuy)
    think(territory) { "New balance ${territory.capital.balance}" }
    return true
  }

  private fun place(territory: Territory, gameInteraction: GameInteraction) {
    val island = territory.island
    val handPiece = island.hand?.piece
    think(territory) { "Placing held piece $handPiece" }
    if (handPiece == null) {
      think(territory) { "Not holding any piece!" }
      return
    }
    if (handPiece is Castle) {
      // hexagons where we can place castles
      val hexagon = calculateBestCastlePlacement(territory)
      if (hexagon == null) {
        think(territory) { "No valid hexagon to place the castle!" }
        return
      }
      think(territory) { "Best placement for this castle is ${hexagon.coordinates}" }
      gameInteraction.click(hexagon)
    } else if (handPiece is LivingPiece) {
      val (mustAttackTrees, mehAttackTrees) = territory.filterIsPiece<TreePiece>().partition {
        val piece = island.getData(it).piece as TreePiece
        // Only care about trees that will spread in our territory
        piece.propagateCandidates(island, it).any { hex -> hex in territory.hexagons }
      }

      val hexagon = run {
        if (mustAttackTrees.isNotEmpty()) {
          think(territory) { "There are ${mustAttackTrees.size} must attack trees in my territory, must tear it down! ${mustAttackTrees.map { it.coordinates }}" }
          return@run mustAttackTrees.random()
        }

        val attackableHexes = attackableHexagons(territory, handPiece)
        if (attackableHexes.isNotEmpty()) {
          think(territory) { "I can attack an enemy hexagon with this piece" }

          fun tryAttack(type: KClass<out Piece>): Hexagon<HexagonData>? {
            val attackableOfType = attackableHexes.filter { type.isInstance(island.getData(it).piece) }
            if (attackableOfType.isNotEmpty()) {
              think(territory) { "Will attack a ${type.simpleName} (there are ${attackableOfType.size} alternatives)" }
              val (hexagonsInTerritory, hexagonsNotInTerritory) = attackableOfType.partition { it.isPartOfATerritory(island) }
              think(territory) {
                "Out of the ${attackableOfType.size} alternatives, ${hexagonsInTerritory.size} hexagons are in a territory, " +
                  "and hexagons ${hexagonsNotInTerritory.size} are not in a territory "
              }
              if (hexagonsInTerritory.isNotEmpty()) {
                think(territory) { "Found ${hexagonsInTerritory.size} attackable hexagons in a territory, will try to attack the hexagon with the highest possible defence" }
                val mostDefendedHex = hexagonsInTerritory.maxBy { island.calculateStrength(it, island.getData(it).team) }
                think(territory) { "The most defended attackable hexagon is ${mostDefendedHex.coordinates}" }
                return mostDefendedHex
              } else {
                think(territory) { "None of the attackable hexagons are in a territory, will attack a random hexagon not in a territory" }
                return hexagonsNotInTerritory.random()
              }
            }
            think(territory) { "Cannot find any ${type.simpleName} to attack" }
            return null
          }

          val connectingHex = attackableHexes.firstOrNull { attackable ->
            island.getNeighbors(attackable)
              .asSequence()
              .filter { hex ->
                hex !in territory.hexagons
              }.any { hex ->
                island.getData(hex).team == territory.team
              }
          }
          think(territory) { "Before attacking: is there a nearby friendly territory? ${connectingHex != null} (${connectingHex?.coordinates})" }

          return@run connectingHex
            ?: tryAttack(Capital::class)
            ?: tryAttack(Castle::class)

            ?: tryAttack(Baron::class)
            ?: tryAttack(Knight::class)
            ?: tryAttack(Spearman::class)
            ?: tryAttack(Peasant::class)

            ?: tryAttack(TreePiece::class)
            // Take over territory which is well defended, also helps with mass attacks
            ?: attackableHexes.maxByOrNull { island.calculateStrength(it, island.getData(it).team) }
            ?: attackableHexes.random()
        }

        think(territory) { "No enemy territory is attackable with held piece" }

        if (mehAttackTrees.isNotEmpty()) {
          think(territory) { "There are ${mehAttackTrees.size} trees in my territory that will not propagate in my territory, ${mehAttackTrees.map { it.coordinates }}" }
          return@run mehAttackTrees.random()
        }

        val graveHexagons = territory.filterIsPiece<Grave>().toSet()
        if (graveHexagons.isNotEmpty()) {
          think(territory) { "But there is a hexagon in my territory with a grave, lets clean it up early" }
          return@run graveHexagons.random()
        }

        val mergeHex = bestPieceToMergeWith(territory, handPiece)
        if (mergeHex != null) {
          think(territory) { "Merging hand with ${island.getData(mergeHex).piece}" }
          resetBlacklists()
          hexPickupPritoryList += mergeHex
          return@run mergeHex
        } else {
          think(territory) { "Found no acceptable pieces to merge held item with" }
        }

        cannotUseStrength(handPiece.strength)
        think(territory) { "Placing the held piece in the best defensive position, nothing else to do" }
        val emptyHex = calculateBestLivingDefencePosition(territory)
        if (emptyHex != null) {
          hexBlacklist += emptyHex
        } else {
          think(territory) { "Failed to place held piece on any hexagons" }
          return
        }
        return@run emptyHex
      }
      think(territory) {
        "Placing piece $handPiece at ${hexagon.coordinates} " +
          "(which is a ${island.getData(hexagon).piece} of team ${island.getData(hexagon).team})"
      }
      gameInteraction.click(hexagon)
    }
  }

  /* *************************************************************************
   *  UTILITY METHODS
   * *************************************************************************/

  private fun allowedToBuyCastle(round: Int): Boolean {
    return round > castleDelay
  }

  private fun shouldBuyCastle(): Boolean {
    return Random.nextDouble() > buyCastleChance
  }

  private fun bestPieceToMergeWith(territory: Territory, handPiece: LivingPiece): Hexagon<HexagonData>? {
    think(territory) { "Will try to merge piece with another piece" }
    val maxBorderStr =
      territory.enemyTerritoryHexagons.maxOfOrNull { territory.island.getData(it).piece.strength }
        ?: NO_STRENGTH

    think(territory) {
      if (maxBorderStr > 0) {
        "The highest level threat on my border has the strength of a ${strengthToType(maxBorderStr).simpleName}"
      } else {
        "I have vanquished my foes!"
      }
    }

    val knights = territory.filterIsPiece<Knight>().count()
    val barons = territory.filterIsPiece<Baron>().count()

    think(territory) { "There are $knights knights and $barons barons in this territory" }

    val disallowKnight = maxBorderStr < SPEARMAN_STRENGTH || knights >= MAX_TOTAL_KNIGHTS - 1
    if (disallowKnight) {
      think(territory) { "Should not merge to knight right now as the threat is not high enough" }
    }

    val disallowBaron = maxBorderStr < KNIGHT_STRENGTH || barons >= MAX_TOTAL_BARONS - 1
    if (disallowBaron) {
      think(territory) { "Should not merge to baron right now as the threat is not high enough" }
    }

    think(territory) { "Trying to find piece to merge held ${handPiece::class.simpleName} with" }

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
      val canMergedAttackAnything = canPieceAttackOrCutDownTree(territory, mergedType)
      val canAttack = !piece.moved && !handPiece.moved && canMergedAttackAnything

      // If we have not yet placed the held piece (ie directly merging it) we do not pay upkeep on it
      val handUpkeep = if (handPiece.data == HexagonData.EDGE_DATA) 0 else handPiece.income

      val newIncome = territory.income - piece.income - handUpkeep + mergedType.income

      think(territory) {
        "Checking if I should merge ${handPiece::class.simpleName} with ${piece::class.simpleName}. " +
          "The merged piece can${if (canAttack) "" else " not"} be used to attack a bordering territory."
      }
      val shouldCreate = canAttack && isEconomicalToCreatePiece(territory.capital.balance, newIncome, true)
      think(territory) {
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
          val piece = island.getData(it).piece
          piece is Empty || (piece is LivingPiece && !piece.moved)
        }
        .associateWith {
          val originData = island.getData(it)
          // We should not consider the piece curr the hexagon itself, as it will not be there when we place the castle
          val filter: (data: HexagonData) -> Boolean = { data ->
            // Living pieces will no
            data.piece !is LivingPiece || data.piece !is Capital || data == originData
          }
          val neighborStrength = island.getNeighbors(it).map { neighbor -> island.calculateStrength(neighbor, filter = filter) }
          island.calculateStrength(it, filter = filter) + (neighborStrength.sum().toDouble() / neighborStrength.size)
        }

    // find any hexagon with that is protected the least
    val minStr = placeableHexes.values.minOrNull() ?: return null
    think(territory) { "The least defended hexagon has a strength of $minStr, castle candidates are ${placeableHexes.mapKeys { it.key.coordinates }}" }

    val leastDefendedHexes =
      placeableHexes.filter { (hex, str) ->
        str <= minStr &&
          // Never place a castle next to another castle
          island.getNeighbors(hex)
            .asSequence()
            .map { island.getData(it) }
            .filter { it.team == team }
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

          val neighborStrength = neighbors.map { neighbor ->
            // how much the strength an enemy neighbor has
            island.getNeighbors(neighbor)
              .asSequence()
              .filter { island.getData(it).team != team && it.isPartOfATerritory(island) } // only count hexagons in actual territories
              .maxOfOrNull { island.calculateStrength(it) }
              ?: 0
          }

          // we don't want to be placed on the border, but if we must then do it where there is the least defence
          if (neighbors.any { island.getData(it).team != team }) {
            return@associateWith -strength
          }

          val normalizedNeighborStrength = neighborStrength.sum().toDouble() / neighborStrength.size
          // subtract current strength of a hex to not overprotect a given hex
          return@associateWith normalizedNeighborStrength - strength
        }

    // find any hexagon with that is protected the least
    val maxStr = placeableHexes.values.maxOrNull() ?: return null

    val leastDefendedHexes =
      placeableHexes.filter { (_, str) -> str >= maxStr }.mapTo(mutableListOf()) { it.key }

    // shuffle the list to make the selection more uniform
    leastDefendedHexes.shuffle()

    if (leastDefendedHexes.size == 1) {
      // Special case if there is only one hexagon to place the piece on
      // We want to try and find a neighbor that is more defended and place the piece there to cover both the piece and the hexagon
      val leastDefendedHexagon = leastDefendedHexes.first()
      val selectedStrength = island.calculateStrength(leastDefendedHexagon)
      val betterDefendedNeighbor = island.getNeighbors(leastDefendedHexagon)
        .asSequence()
        .filter {
          val data = island.getData(it)
          data.team == team &&
            data.piece is Empty &&
            island.calculateStrength(it) > selectedStrength
        }
        .maxByOrNull { island.calculateStrength(it) }
      think(territory) {
        if (betterDefendedNeighbor != null) {
          "Was going to place piece defensively at ${leastDefendedHexagon.coordinates}, " +
            "but the neighbor ${betterDefendedNeighbor.coordinates} is better defended and still covers the least defended hexagon"
        } else {
          "Placing piece defensively at ${leastDefendedHexagon.coordinates}, found no better defended neighbor we can place the piece on"
        }
      }
      return betterDefendedNeighbor ?: leastDefendedHexagon
    }
    think(territory) { "There are ${leastDefendedHexes.size} equally badly defended hexagons to place the piece on" }

    // there are multiple hexagons that are defended as badly, choose the hexagon that will protect
    // the most hexagons
    return leastDefendedHexes.maxByOrNull {
      val neighbors = island.getNeighbors(it)
      neighbors.count { neighbor -> island.getData(neighbor).team == team } / neighbors.size
    }
  }

  private fun attackableHexagons(territory: Territory, piece: LivingPiece): List<Hexagon<HexagonData>> {
    return territory.enemyBorderHexes.filter { territory.island.canAttack(it, piece) }
  }

  private fun canPieceAttackOrCutDownTree(territory: Territory, piece: LivingPiece): Boolean {
    return territory.enemyBorderHexes.any { hexagon -> territory.island.canAttack(hexagon, piece) } ||
      territory.filterIsPiece<TreePiece>().any()
  }

  private fun canAttackOrMergePiece(territory: Territory, piece: LivingPiece): Boolean {
    if (canPieceAttackOrCutDownTree(territory, piece)) {
      return true
    }
    val mergeWith = bestPieceToMergeWith(territory, piece) ?: return false
    val mergePiece = territory.island.getData(mergeWith).piece
    require(mergePiece is LivingPiece) { "Merge piece is not a living piece" }
    val mergedType = mergedType(piece, mergePiece).createHandInstance()
    return canPieceAttackOrCutDownTree(territory, mergedType)
  }

  private fun existsEmptyHexagon(territory: Territory): Boolean = territory.filterIsPiece<Empty>().any()

  private fun isEconomicalToBuyPiece(territory: Territory, piece: LivingPiece): Boolean {
    // only buy pieces we can maintain for at least PIECE_MAINTAIN_CONTRACT_LENGTH turns
    val newBalance = territory.capital.balance - piece.price
    val newIncome = territory.income + piece.income
    return territory.capital.canBuy(piece) && isEconomicalToCreatePiece(
      newBalance,
      newIncome,
      canPieceAttackOrCutDownTree(territory, piece)
    )
  }

  companion object {

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