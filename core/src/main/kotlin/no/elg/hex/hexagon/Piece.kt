package no.elg.hex.hexagon

import com.badlogic.gdx.Gdx
import kotlin.reflect.KClass
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.util.connectedHexagons
import no.elg.hex.util.createInstance
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.treeType
import org.hexworks.mixite.core.api.Hexagon

const val NO_STRENGTH = 0

const val PEASANT_STRENGTH = 1

const val SPEARMAN_STRENGTH = 2

const val KNIGHT_STRENGTH = 3

const val BARON_STRENGTH = 4

fun strengthToType(str: Int): KClass<out LivingPiece> {
  return when (str) {
    PEASANT_STRENGTH -> Peasant::class
    SPEARMAN_STRENGTH -> Spearman::class
    KNIGHT_STRENGTH -> Knight::class
    BARON_STRENGTH -> Baron::class
    else ->
        error(
            "Invalid strength level '$str', must be between $PEASANT_STRENGTH and $BARON_STRENGTH (both inclusive)")
  }
}

fun mergedType(piece1: LivingPiece, piece2: LivingPiece) =
    strengthToType(piece1.strength + piece2.strength)

enum class CapitalPlacementPreference {
  /**
   * Will be preferred over any other choice
   *
   * @see Empty
   */
  STRONGLY,

  /** Not ideal, but better than [LAST_RESORT] */
  WEAKLY,

  /** Can only be overwritten as a last resort */
  LAST_RESORT
}

/// **
// * Only return the pices of the strongest preferences. F.eks if any [STRONGLY] they are always
// returned. If no [STRONGLY] are presenent but one [LAST_RESORT] and one [WEAKLY] are present only
// [WEAKLY] will be returned
// */
// fun filterToCapitalPreference(hexagons: Iterable<Hexagon<HexagonData>>):
// Set<Hexagon<HexagonData>> {
//  val highestPref = hexagons.mapTo(HashSet()) { it.getData(island).piece.capitalPlacement }
//  if(h)
// }

/** @author Elg */
sealed class Piece {

  abstract val data: HexagonData

  /** How powerful an object is. Must be Strictly positive */
  abstract val strength: Int

  /** Can this object move once placed */
  abstract val movable: Boolean

  /** How much the territory gain/lose by maintaining this piece */
  abstract val cost: Int

  open val price: Int
    get() = error("This piece cannot be bought!")

  open val capitalPlacement: CapitalPlacementPreference = CapitalPlacementPreference.LAST_RESORT

  open val canBePlacedOn: Array<KClass<out Piece>> = emptyArray()

  /**
   * Action called when this is placed on an hexagon
   *
   * @param onto The hexagon this is placed onto
   *
   * @return If the placement was successful
   */
  open fun place(onto: HexagonData): Boolean {
    return if (!Hex.args.mapEditor &&
        (!(onto.piece is Empty || canBePlacedOn.any { it.isInstance(onto.piece) }))) {
      Gdx.app.debug(
          "${this::class.simpleName}-${this::place.name}",
          "Piece ${this::class.simpleName} can only be placed on Empty or ${canBePlacedOn.map { it::simpleName }} pieces. Tried to place it on ${onto.piece::class.simpleName}")
      false
    } else {
      true
    }
  }

  override fun toString(): String = this::class.simpleName!!

  var elapsedAnimationTime = 0f

  /**
   * Called when each team ends it's turn. The [pieceHex], [data], and [team] are guaranteed to be
   * synced. Only hexagons who's turn it is will be called.
   */
  open fun beginTurn(
      island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team
  ) {
    // NO-OP
  }

  /** Called when all visible hexagons's [beginTurn] has been called */
  open fun newRound(island: Island, pieceHex: Hexagon<HexagonData>) {
    // NO-OP
  }
}

val PIECES: List<KClass<out Piece>> by lazy {
  val subclasses = ArrayList<KClass<out Piece>>()

  fun addAllSubclasses(it: KClass<out Piece>) {
    if (it.isSealed) {
      for (subclass in it.sealedSubclasses) {
        addAllSubclasses(subclass)
      }
    } else {
      subclasses += it
    }
  }

  addAllSubclasses(Piece::class)
  return@lazy subclasses
}

///////////
// Empty //
///////////

object Empty : Piece() {
  override val strength: Int = NO_STRENGTH
  override val movable: Boolean = false
  override val data: HexagonData
    get() = error("Empty Piece is not confined to a Hexagon")
  override val capitalPlacement = CapitalPlacementPreference.STRONGLY
  override val cost: Int = 1
  override fun place(onto: HexagonData): Boolean = true
}

////////////////
// Stationary //
////////////////

sealed class StationaryPiece(final override val data: HexagonData) : Piece() {

  private var placed: Boolean = false

  final override val movable: Boolean = false

  override fun place(onto: HexagonData): Boolean {
    require(!placed) { "Stationary pieces can only be placed once" }
    if (!super.place(onto)) return false
    placed = true
    return true
  }

  override fun toString(): String {
    return "${this::class.simpleName}(placed? $placed)"
  }
}

class Capital(data: HexagonData) : StationaryPiece(data) {

  /** How much money this territory currently has */
  var balance: Int = 0
  override val cost: Int = 1
  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(Piece::class)
  override val strength = PEASANT_STRENGTH

  /** Transfer everything to the given capital */
  fun transfer(to: Capital) {
    to.balance += balance
    balance = 0
  }

  private fun killall(island: Island, iterable: Iterable<Hexagon<HexagonData>>) {
    for (hex in iterable) {
      val piece = island.getData(hex).piece
      if (piece is LivingPiece) {
        piece.kill(island, hex)
      }
    }
  }

  override fun beginTurn(
      island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team
  ) {
    val hexagons = island.getTerritoryHexagons(pieceHex)

    if (hexagons == null) {
      killall(island, island.connectedHexagons(pieceHex))
      island.getData(pieceHex).setPiece(island.treeType(pieceHex))
      return
    }

    require(island.getCapitalOf(hexagons) === this) {
      "Mismatch between this piece and the capital of the territory!"
    }

    balance += calculateIncome(hexagons, island)
    if (balance < 0) {
      killall(island, hexagons)
    }
  }

  fun calculateIncome(hexagons: Iterable<Hexagon<HexagonData>>, island: Island) =
      hexagons.sumBy { island.getData(it).piece.cost }

  fun canBuy(piece: KClass<out Piece>): Boolean =
      canBuy(piece.createInstance(HexagonData.EDGE_DATA))

  fun canBuy(piece: Piece): Boolean = balance >= piece.price

  override fun toString(): String = "${this::class.simpleName}(balance: $balance)"
}

class Castle(data: HexagonData) : StationaryPiece(data) {
  override val strength = SPEARMAN_STRENGTH
  override val cost: Int = 1
  override val price: Int = 15
}

class Grave(data: HexagonData) : StationaryPiece(data) {
  override val strength = NO_STRENGTH
  override val cost: Int = 1

  private var roundsToTree: Byte = 1

  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(LivingPiece::class)

  override fun newRound(island: Island, pieceHex: Hexagon<HexagonData>) {
    if (roundsToTree > 0) {
      roundsToTree--
      return
    }
    island.getData(pieceHex).setPiece(island.treeType(pieceHex))
  }

  override fun toString(): String = "${this::class.simpleName}(timeToTree: $roundsToTree)"
}

//////////
// TREE //
//////////

sealed class TreePiece(data: HexagonData) : StationaryPiece(data) {

  var hasGrown: Boolean = true

  override val capitalPlacement = CapitalPlacementPreference.WEAKLY
  override val strength = NO_STRENGTH
  override val cost: Int = 0
  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(Capital::class, Grave::class)

  override fun beginTurn(
      island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team
  ) {
    hasGrown = false
  }
}

class PineTree(data: HexagonData) : TreePiece(data) {
  override fun newRound(island: Island, pieceHex: Hexagon<HexagonData>) {
    if (hasGrown) return

    // Find all empty neighbor hexes that are empty
    val list =
        island
            .getNeighbors(pieceHex)
            .filter {
              val piece = island.getData(it).piece
              piece is Empty && island.treeType(it) == PineTree::class
            }
            .shuffled()

    for (hexagon in list) {
      // Find all neighbor hexes (of our selected neighbor) has a pine next to it that has yet to
      // grow
      val otherPines =
          island.getNeighbors(hexagon).filter {
            if (it == pieceHex) return@filter false
            val piece = island.getData(it).piece
            return@filter piece is PineTree && !piece.hasGrown && it != pieceHex
          }
      if (otherPines.isNotEmpty()) {
        // Grow a tree between this pine and another pine
        val otherPine = island.getData(otherPines.random()).piece as TreePiece
        val loopData = island.getData(hexagon)
        if (loopData.setPiece(PineTree::class)) {
          hasGrown = true
          otherPine.hasGrown = true
          (loopData.piece as TreePiece).hasGrown = true
          break
        }
      }
    }
  }
}

class PalmTree(data: HexagonData) : TreePiece(data) {

  override fun newRound(island: Island, pieceHex: Hexagon<HexagonData>) {
    if (hasGrown) return
    // Find all empty neighbor hexes that are empty along the cost
    val hex =
        island
            .getNeighbors(pieceHex)
            .filter {
              val piece = island.getData(it).piece
              piece is Empty && island.treeType(it) == PalmTree::class
            }
            .randomOrNull()
            ?: return

    val randomNeighborData = island.getData(hex)

    if (randomNeighborData.setPiece(PalmTree::class)) {
      (randomNeighborData.piece as TreePiece).hasGrown = true
      hasGrown = true
    }
  }
}

////////////
// LIVING //
////////////

sealed class LivingPiece(final override val data: HexagonData) : Piece() {

  final override val movable: Boolean = true

  var moved: Boolean = true

  override val canBePlacedOn: Array<KClass<out Piece>> =
      arrayOf(LivingPiece::class, StationaryPiece::class)

  fun kill(island: Island, hex: Hexagon<HexagonData>) {
    island.getData(hex).setPiece(Grave::class)
  }

  override fun newRound(island: Island, pieceHex: Hexagon<HexagonData>) {
    if (island.getTerritoryHexagons(pieceHex) == null) {
      kill(island, pieceHex)
    } else {
      moved = false
    }
  }

  fun canNotMerge(with: LivingPiece): Boolean {
    return this.strength + with.strength > BARON_STRENGTH
  }

  fun updateAnimationTime(): Float {
    if (moved) {
      elapsedAnimationTime = 0f
    } else {
      elapsedAnimationTime += Gdx.graphics.deltaTime
    }
    return elapsedAnimationTime
  }

  override fun toString(): String = "${this::class.simpleName}(moved? $moved)"
}

class Peasant(data: HexagonData) : LivingPiece(data) {
  override val strength = PEASANT_STRENGTH
  override val cost: Int = -1
  override val price: Int = 10
}

class Spearman(data: HexagonData) : LivingPiece(data) {
  override val strength = SPEARMAN_STRENGTH
  override val cost: Int = -5
  override val price: Int = 20
}

class Knight(data: HexagonData) : LivingPiece(data) {
  override val strength = KNIGHT_STRENGTH
  override val cost: Int = -17
  override val price: Int = 30
}

class Baron(data: HexagonData) : LivingPiece(data) {
  override val strength = BARON_STRENGTH
  override val cost: Int = -53
  override val price: Int = 40
}
