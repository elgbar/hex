package no.elg.hex.hexagon

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.MAX_START_CAPITAL
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.debug
import no.elg.hex.util.error
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.isNotPartOfATerritory
import no.elg.hex.util.trace
import no.elg.hex.util.treeType
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.KClass

const val NO_STRENGTH = 0
const val PEASANT_STRENGTH = 1
const val SPEARMAN_STRENGTH = 2
const val KNIGHT_STRENGTH = 3
const val BARON_STRENGTH = 4

const val CASTLE_PRICE = 15
const val PEASANT_PRICE = 10

const val DESER_TAG = "PIECE DESER"

fun strengthToType(str: Int): KClass<out LivingPiece> {
  return when (str) {
    PEASANT_STRENGTH -> Peasant::class
    SPEARMAN_STRENGTH -> Spearman::class
    KNIGHT_STRENGTH -> Knight::class
    BARON_STRENGTH -> Baron::class
    else -> error("Invalid strength level '$str', must be between $PEASANT_STRENGTH and $BARON_STRENGTH (both inclusive)")
  }
}

fun strengthToTypeOrNull(str: Int): KClass<out LivingPiece>? {
  if (str !in PEASANT_STRENGTH..BARON_STRENGTH) {
    return null
  }
  return strengthToType(str)
}

fun mergedType(piece1: LivingPiece, piece2: LivingPiece) = strengthToType(piece1.strength + piece2.strength)

fun replaceWithTree(island: Island, hex: Hexagon<HexagonData>) {
  val data = island.getData(hex)
  val pieceType = island.treeType(hex)
  Gdx.app.trace("Piece") { "Replacing piece ${data.piece} (${hex.gridX},${hex.gridZ}) with tree $pieceType" }
  data.setPiece(pieceType)
}

/** @author Elg */
sealed class Piece {

  abstract val data: HexagonData

  /** How powerful a piece is. Must be Strictly positive */
  abstract val strength: Int

  /** Can this object move once placed */
  abstract val movable: Boolean

  /** How much the territory gain/lose by maintaining this piece*/
  open val income: Int = 1

  open val price: Int
    get() = error("This piece cannot be bought!")

  /**
   * How strongly this piece prefer not to be overwritten by a capital.
   * The higher the value the more it does not want to be overwritten
   */
  abstract val capitalReplacementResistance: Int

  open val canBePlacedOn: Array<KClass<out Piece>> = emptyArray()

  /**
   * Action called when this is placed on an hexagon
   *
   * @param onto The hexagon this is placed onto
   *
   * @return If the placement was successful
   */
  open fun place(onto: HexagonData): Boolean {
    return if (Hex.args.mapEditor || onto.piece is Empty || canBePlacedOn.any { it.isInstance(onto.piece) }) {
      true
    } else {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}") {
        "Piece ${this::class.simpleName} can only be placed on Empty or ${
          canBePlacedOn.joinToString {
            it.simpleName ?: "??"
          }
        } pieces. Tried to place it on ${onto.piece::class.simpleName}"
      }
      false
    }
  }

  abstract fun copyTo(newData: HexagonData): Piece

  override fun toString(): String = this::class.simpleName!!

  var elapsedAnimationTime = 0f

  /**
   * Called when each team ends it's turn. The [pieceHex], [data], and [team] are guaranteed to be
   * synced. Only hexagons who's turn it is will be called.
   */
  open fun beginTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) = Unit

  /** Called when all visible hexagons' [beginTurn] has been called */
  open fun newRound(island: Island, pieceHex: Hexagon<HexagonData>) = Unit

  open val serializationData: Any? = null

  open fun handleDeserializationData(serializationData: Any?) = Unit
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

val PIECES_MAP: Map<String?, KClass<out Piece>> by lazy { PIECES.associateBy { it.qualifiedName } }

// /////////
// Empty //
// /////////

object Empty : Piece() {
  override val strength: Int = NO_STRENGTH
  override val movable: Boolean = false
  override val data: HexagonData
    get() = error("Empty Piece is not confined to a Hexagon")
  override val capitalReplacementResistance = -1
  override fun place(onto: HexagonData): Boolean = true
  override fun copyTo(newData: HexagonData) = this
}

// //////////////
// Stationary //
// //////////////

sealed class StationaryPiece(final override val data: HexagonData, protected var placed: Boolean) : Piece() {

  final override val movable: Boolean = false

  override fun place(onto: HexagonData): Boolean {
    require(!placed) { "Stationary pieces can only be placed once" }
    if (!super.place(onto)) return false
    placed = true
    return true
  }

  override fun toString() = "${this::class.simpleName}(placed? $placed)"
}

class Capital(data: HexagonData, placed: Boolean = false, var balance: Int = 0) : StationaryPiece(data, placed) {

  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(Piece::class)
  override val strength = PEASANT_STRENGTH
  override val capitalReplacementResistance: Int = Int.MIN_VALUE

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
    island: Island,
    pieceHex: Hexagon<HexagonData>,
    data: HexagonData,
    team: Team
  ) {
    val hexagons = island.getTerritoryHexagons(pieceHex)
    if (hexagons == null) {
      replaceWithTree(island, pieceHex)
      return
    }

    require(island.findCapital(hexagons) === this) { "Mismatch between this piece and the capital of the territory!" }

    if (island.round > 1) {
      balance += calculateIncome(hexagons, island)
    }
    if (balance < 0) {
      killall(island, hexagons)
      balance = 0
    }
  }

  fun calculateIncome(hexagons: Iterable<Hexagon<HexagonData>>, island: Island) = hexagons.sumOf { island.getData(it).piece.income }

  fun canBuy(piece: KClass<out Piece>): Boolean = canBuy(piece.createHandInstance())

  fun canBuy(piece: Piece): Boolean = balance >= piece.price

  fun calculateStartCapital(hexagons: Iterable<Hexagon<HexagonData>>, island: Island): Int {
    return hexagons.sumOf { Island.START_CAPITAL_PER_HEX + (island.getData(it).piece.income - 1) }.coerceAtMost(MAX_START_CAPITAL)
  }

  override fun copyTo(newData: HexagonData): Capital {
    return Capital(newData, placed, balance)
  }

  override fun toString(): String = "${this::class.simpleName}(balance: $balance)"

  override val serializationData: Int get() = balance

  override fun handleDeserializationData(serializationData: Any?) {
    if (serializationData is Number) {
      Gdx.app.trace(DESER_TAG) { "Setting balance of $this to $serializationData" }
      balance = serializationData.toInt()
    } else if (serializationData != null) {
      Gdx.app.error(DESER_TAG) {
        "The piece $this have the wrong type of serialization data. Expected a number or null, but got ${serializationData::class}"
      }
    }
  }
}

class Castle(data: HexagonData, placed: Boolean = false) : StationaryPiece(data, placed) {
  override val strength = SPEARMAN_STRENGTH
  override val price: Int = CASTLE_PRICE
  override val capitalReplacementResistance: Int = Int.MAX_VALUE
  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(LivingPiece::class)

  override fun copyTo(newData: HexagonData): Castle {
    return Castle(newData, placed)
  }
}

class Grave(data: HexagonData, placed: Boolean = false) : StationaryPiece(data, placed) {

  override val capitalReplacementResistance = 0
  override val strength = NO_STRENGTH

  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(LivingPiece::class)

  override fun beginTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    replaceWithTree(island, pieceHex)
  }

  override fun copyTo(newData: HexagonData): Grave {
    return Grave(newData, placed)
  }
}

// ////////
// TREE //
// ////////

sealed class TreePiece(data: HexagonData, placed: Boolean, var lastGrownTurn: Int) : StationaryPiece(data, placed) {

  override val capitalReplacementResistance = 0
  override val strength = NO_STRENGTH
  override val income: Int = 0
  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(Capital::class, Grave::class)

  /**
   * Create a new tree according to the tree types rules
   */
  protected abstract fun propagate(island: Island, pieceHex: Hexagon<HexagonData>)

  override fun beginTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    if (hasGrown) {
      Gdx.app.trace("Tree", "Tree has already grown this round, skipping it")
      return
    }
    propagate(island, pieceHex)
  }

  fun markAsGrown() {
    lastGrownTurn = Hex.island?.turn ?: 0
  }

  val hasGrown: Boolean get() = Hex.island?.turn == lastGrownTurn

  override fun toString() = "${this::class.simpleName}(placed? $placed, lastGrownRound: $lastGrownTurn)"

  override val serializationData: Int
    get() = lastGrownTurn

  override fun handleDeserializationData(serializationData: Any?) {
    if (serializationData is Int) {
      Gdx.app.trace(DESER_TAG, "Updating moved of $this to $serializationData")
      lastGrownTurn = serializationData
    } else if (serializationData != null) {
      Gdx.app.error(DESER_TAG, "The piece $this have the wrong type of serialization data. Expected a int or null, but got ${serializationData::class}")
    }
  }
}

class PineTree(data: HexagonData, placed: Boolean = false, lastGrownTurn: Int = 0) : TreePiece(data, placed, lastGrownTurn) {

  override fun propagate(island: Island, pieceHex: Hexagon<HexagonData>) {
    // Find all empty neighbor hexes that are empty
    val emptyNeighbors = island.getNeighbors(pieceHex).filter {
      island.getData(it).piece is Empty && island.treeType(it) == PineTree::class
    }.shuffled()

    for (hexagon in emptyNeighbors) {
      // Find all neighbor hexes (of our selected neighbor) has a pine next to it that has yet to
      // grow
      val otherPines = island.getNeighbors(hexagon).filter {
        if (it == pieceHex) return@filter false
        val piece = island.getData(it).piece
        return@filter piece is PineTree && !piece.hasGrown
      }
      if (otherPines.isNotEmpty()) {
        // Grow a tree between this pine and another pine
        val neighborPine = island.getData(otherPines.random()).piece as TreePiece
        val placed = island.getData(hexagon).setPiece<PineTree> { it.markAsGrown() }
        if (placed) {
          this@PineTree.markAsGrown()
          neighborPine.markAsGrown()
          break
        }
      }
    }
  }

  override fun copyTo(newData: HexagonData): PineTree {
    return PineTree(newData, placed, lastGrownTurn)
  }
}

class PalmTree(data: HexagonData, placed: Boolean = false, lastGrownTurn: Int = 0) : TreePiece(data, placed, lastGrownTurn) {

  override fun propagate(island: Island, pieceHex: Hexagon<HexagonData>) {
    // Find all empty neighbor hexes that are empty along the cost
    val neighbour = island.getNeighbors(pieceHex)
      .filter { island.getData(it).piece is Empty && island.treeType(it) == PalmTree::class }
      .randomOrNull() ?: return

    island.getData(neighbour).setPiece<PalmTree> {
      it.markAsGrown()
      this.markAsGrown()
    }
  }

  override fun copyTo(newData: HexagonData): PalmTree {
    return PalmTree(newData, placed, lastGrownTurn)
  }
}

// //////////
// LIVING //
// //////////

sealed class LivingPiece(final override val data: HexagonData, var moved: Boolean) : Piece() {

  final override val movable: Boolean = true

  override val canBePlacedOn: Array<KClass<out Piece>> = PLACEABLE_CLASSES

  fun kill(island: Island, hex: Hexagon<HexagonData>) {
    Gdx.app.trace("Piece") { "Replacing piece ${data.piece} (${hex.gridX},${hex.gridZ}) with a grave" }
    island.getData(hex).setPiece(Grave::class)
  }

  override fun beginTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    if (pieceHex.isNotPartOfATerritory(island)) {
      kill(island, pieceHex)
    } else {
      moved = false
    }
  }

  fun canNotMerge(with: LivingPiece): Boolean {
    return this.strength + with.strength > BARON_STRENGTH
  }

  inline fun canMerge(with: LivingPiece): Boolean {
    return !canNotMerge(with)
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

  override val serializationData: Boolean
    get() = moved

  override fun handleDeserializationData(serializationData: Any?) {
    if (serializationData is Boolean) {
      Gdx.app.trace(DESER_TAG, "Updating moved of $this to $serializationData")
      moved = serializationData
    } else if (serializationData != null) {
      Gdx.app.error(
        DESER_TAG,
        "The piece $this have the wrong type of serialization data. Expected a boolean or null, but got ${serializationData::class}"
      )
    }
  }

  companion object {
    val PLACEABLE_CLASSES = arrayOf(LivingPiece::class, StationaryPiece::class)
  }
}

class Peasant(data: HexagonData, moved: Boolean = true) : LivingPiece(data, moved) {
  override val strength = PEASANT_STRENGTH
  override val income: Int = -1
  override val capitalReplacementResistance: Int = 4
  override val price: Int = PEASANT_PRICE
  override fun copyTo(newData: HexagonData): Peasant {
    return Peasant(newData, moved)
  }
}

class Spearman(data: HexagonData, moved: Boolean = true) : LivingPiece(data, moved) {
  override val strength = SPEARMAN_STRENGTH
  override val income: Int = -5
  override val capitalReplacementResistance: Int = 3
  override val price: Int = 20
  override fun copyTo(newData: HexagonData): Spearman {
    return Spearman(newData, moved)
  }
}

class Knight(data: HexagonData, moved: Boolean = true) : LivingPiece(data, moved) {
  override val strength = KNIGHT_STRENGTH
  override val income: Int = -17
  override val capitalReplacementResistance: Int = 2
  override val price: Int = 30
  override fun copyTo(newData: HexagonData): Knight {
    return Knight(newData, moved)
  }
}

class Baron(data: HexagonData, moved: Boolean = true) : LivingPiece(data, moved) {
  override val strength = BARON_STRENGTH
  override val income: Int = -53
  override val capitalReplacementResistance: Int = 1
  override val price: Int = 40
  override fun copyTo(newData: HexagonData): Baron {
    return Baron(newData, moved)
  }
}