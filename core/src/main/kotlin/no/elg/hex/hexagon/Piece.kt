package no.elg.hex.hexagon

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import no.elg.hex.event.Events
import no.elg.hex.event.events.CapitalBalanceChanged
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.MAX_START_CAPITAL
import no.elg.hex.util.coordinates
import no.elg.hex.util.createHandInstance
import no.elg.hex.util.debug
import no.elg.hex.util.error
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.idealTreeType
import no.elg.hex.util.isIdealTreeType
import no.elg.hex.util.isNotPartOfATerritory
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon
import kotlin.contracts.contract
import kotlin.reflect.KClass

const val NO_STRENGTH = 0
const val PEASANT_STRENGTH = 1
const val SPEARMAN_STRENGTH = 2
const val KNIGHT_STRENGTH = 3
const val BARON_STRENGTH = 4

const val CASTLE_PRICE = 15
const val PEASANT_PRICE = 10

const val PEASANT_UPKEEP_COST = -2

const val DESER_TAG = "PIECE DESER"

fun strengthToType(str: Int): KClass<out LivingPiece> =
  when (str) {
    PEASANT_STRENGTH -> Peasant::class
    SPEARMAN_STRENGTH -> Spearman::class
    KNIGHT_STRENGTH -> Knight::class
    BARON_STRENGTH -> Baron::class
    else -> error("Invalid strength level '$str', must be between $PEASANT_STRENGTH and $BARON_STRENGTH (both inclusive)")
  }

fun strengthToTypeOrNull(str: Int): KClass<out LivingPiece>? {
  if (str !in PEASANT_STRENGTH..BARON_STRENGTH) {
    return null
  }
  return strengthToType(str)
}

fun mergedType(piece1: LivingPiece, piece2: LivingPiece) = strengthToType(piece1.strength + piece2.strength)

fun replaceWithTree(island: Island, hex: Hexagon<HexagonData>): Boolean {
  val data = island.getData(hex)
  val pieceType = island.idealTreeType(hex)
  Gdx.app.trace("Piece") { "Replacing piece ${data.piece} (${hex.coordinates}) with tree $pieceType" }
  return data.setPiece(pieceType) {
    require(!it.canGrowThisTurn) {
      "Tree being placed by another tree should already be marked as grown"
    }
  }
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

  /**
   * Which types of pieces can this be placed on top of in normal gameplay. The [Empty] piece is always allowed, and should be omitted
   */
  open val canBePlacedOn: Array<KClass<out Piece>> = emptyArray()

  /**
   * Action called when this is placed on an hexagon
   *
   * @param onto The hexagon this is placed onto
   *
   * @return If the placement was successful
   */
  open fun place(onto: HexagonData): Boolean =
    if (Hex.mapEditor || onto.piece is Empty || canBePlacedOn.any { it.isInstance(onto.piece) }) {
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

  abstract fun copyTo(newData: HexagonData): Piece

  override fun toString(): String = this::class.simpleName!!

  var elapsedAnimationTime = 0f

  /**
   * Called for each visible hexagon at the beginning of every turn. Will be called just before [beginTeamTurn]
   *
   * Difference between [newTurn] and [beginTeamTurn] is that [newTurn] is called for all visible hexagons regardless of what team's turn it is.
   */
  open fun newTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData) = Unit

  /**
   * Called when each team ends it's turn. The [pieceHex], [data], and [team] are guaranteed to be
   * synced. Only hexagons whose turn it is will be called.
   */
  open fun beginTeamTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) = Unit

  open val serializationData: Any? = null

  open fun handleDeserializationData(serializationData: Any?) = Unit
  override fun equals(other: Any?): Boolean = equalsWithoutData(other) && data == other.data

  fun equalsWithoutData(other: Any?): Boolean {
    contract { returns(true) implies (other is Piece) }
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Piece

    return serializationData == other.serializationData
  }

  override fun hashCode(): Int {
    var result = 1
    result = 31 * result + (serializationData?.hashCode() ?: 0)
    return result
  }
}

val PIECES: List<KClass<out Piece>> by lazy {
  val subclasses = ArrayList<KClass<out Piece>>()
  forEachPieceSubClass { subclasses += it }
  return@lazy subclasses
}

val PIECES_ORGANIZED: List<List<KClass<out Piece>>> by lazy {
  val subclassesRow = mutableListOf<MutableList<KClass<out Piece>>>()
  var subclasses: MutableList<KClass<out Piece>> = mutableListOf()
  val sealed: (KClass<out Piece>) -> Unit = {
    subclasses = mutableListOf<KClass<out Piece>>().also { subclassesRow.add(it) }
  }
  val instance: (KClass<out Piece>) -> Unit = {
    subclasses += it
  }
  forEachPieceSubClass(sealed, instance)
  return@lazy subclassesRow
}

fun forEachPieceSubClass(sealed: (KClass<out Piece>) -> Unit = {}, instance: (KClass<out Piece>) -> Unit) {
  fun addAllSubclasses(pieceKClass: KClass<out Piece>) {
    when {
      pieceKClass.isSealed -> {
        sealed(pieceKClass)
        pieceKClass.sealedSubclasses.forEach { addAllSubclasses(it) }
      }

      else -> instance(pieceKClass)
    }
  }
  addAllSubclasses(Piece::class)
}

val PIECES_MAP: Map<String?, KClass<out Piece>> by lazy {
  PIECES.associateBy { it.simpleName } + PIECES.associateBy { it.qualifiedName }
}

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
  override fun equals(other: Any?): Boolean = other === this

  override fun hashCode(): Int = 0
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

class Capital(data: HexagonData, placed: Boolean = false, balance: Int = 0) : StationaryPiece(data, placed) {

  var balance: Int = balance
    set(value) {
      if (field == value) return

      val old = field
      field = value
      Events.fireEvent(CapitalBalanceChanged(data, old, value))
    }

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

  override fun beginTeamTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    val hexagons = island.getTerritoryHexagons(pieceHex)
    if (hexagons == null) {
      replaceWithTree(island, pieceHex)
      return
    }

    val findOrCreateCapital = island.findOrCreateCapital(hexagons)
    requireNotNull(findOrCreateCapital) { "Failed to find or create a capital for the territory" }
    require(island.getData(findOrCreateCapital).piece === this) { "Mismatch between this piece and the capital of the territory!" }

    if (island.round > 1) {
      balance += calculateIncome(hexagons, island)
    }
    if (balance < 0) {
      killall(island, hexagons)
      balance = 0
    }
  }

  fun calculateIncome(hexagons: Iterable<Hexagon<HexagonData>>, island: Island) = hexagons.sumOf { island.getData(it).piece.income }

  inline fun <reified T : Piece> canBuy(): Boolean = canBuy(T::class.createHandInstance())

  fun canBuy(piece: Piece): Boolean = balance >= piece.price

  fun calculateStartCapital(hexagons: Iterable<Hexagon<HexagonData>>, island: Island): Int =
    hexagons.sumOf {
      Island.START_CAPITAL_PER_HEX + (island.getData(it).piece.income - 1)
    }.coerceAtMost(MAX_START_CAPITAL)

  override fun copyTo(newData: HexagonData): Capital = Capital(newData, placed, balance)

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

  override fun copyTo(newData: HexagonData): Castle = Castle(newData, placed)
}

class Grave(data: HexagonData, placed: Boolean = false) : StationaryPiece(data, placed) {

  override val capitalReplacementResistance = 0
  override val strength = NO_STRENGTH

  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(LivingPiece::class)

  override fun beginTeamTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    replaceWithTree(island, pieceHex)
  }

  override fun copyTo(newData: HexagonData): Grave = Grave(newData, placed)
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
   * Create a new tree according to the tree types rules.
   *
   * @return if the tree successfully propagated
   *
   */
  protected abstract fun propagate(island: Island, pieceHex: Hexagon<HexagonData>): Boolean

  /**
   * What hexagons the tree can propagate to
   */
  abstract fun propagateCandidates(island: Island, pieceHex: Hexagon<HexagonData>): Sequence<Hexagon<HexagonData>>

  override fun newTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData) {
    if (canGrowThisTurn) {
      Gdx.app.trace("Tree") { "Tree can propagate this round! last grown $lastGrownTurn (current turn: ${currentTurn()})" }
      if (propagate(island, pieceHex)) {
        markAsGrown()
      }
    } else {
      Gdx.app.trace("Tree") {
        "Tree has already grown this round, skipping it, last grown $lastGrownTurn. current turn is ${currentTurn()}. scheduled to grow on turn ${lastGrownTurn + Team.entries.size}"
      }
    }
  }

  fun markAsGrown() {
    lastGrownTurn = currentTurn()
  }

  /**
   * @return whether this tree can grow this turn
   */
  val canGrowThisTurn: Boolean get() = lastGrownTurn + Team.entries.size <= currentTurn()

  override fun toString() = "${this::class.simpleName}(placed? $placed, lastGrownTurn: $lastGrownTurn)"

  override val serializationData: Int
    get() = lastGrownTurn

  override fun handleDeserializationData(serializationData: Any?) {
    if (serializationData is Int) {
      if (serializationData == 0 && data.team.ordinal != 0) {
        Gdx.app.trace(DESER_TAG) { "serializationData was 0. Updating to fix grow rate bug. Setting to the hex team's ordinal: ${data.team.ordinal}" }
        lastGrownTurn = data.team.ordinal
      } else {
        Gdx.app.trace(DESER_TAG) { "Updating moved of $this to $serializationData" }
        lastGrownTurn = serializationData
      }
    } else if (serializationData != null) {
      Gdx.app.error(DESER_TAG, "The piece $this have the wrong type of serialization data. Expected a int or null, but got ${serializationData::class}")
    }
  }

  companion object {

    @JvmStatic
    protected fun currentTurn(): Int = Hex.island?.turn ?: 0
  }
}

class PineTree(data: HexagonData, placed: Boolean = false, lastGrownTurn: Int = currentTurn()) : TreePiece(data, placed, lastGrownTurn) {

  override fun propagateCandidates(island: Island, pieceHex: Hexagon<HexagonData>): Sequence<Hexagon<HexagonData>> = propagateCandidates0(island, pieceHex).map { it.first }

  /**
   * @return A sequence of pairs where the first is the hexagon to grow into, and the second is the other pine tree that caused the growth
   */
  private fun propagateCandidates0(island: Island, pieceHex: Hexagon<HexagonData>): Sequence<Pair<Hexagon<HexagonData>, Hexagon<HexagonData>>> =
    island.getNeighbors(pieceHex)
      .asSequence()
      .filter {
        island.getData(it).piece is Empty && island.isIdealTreeType<PineTree>(it)
      }.flatMap { neighbor ->
        // Find all neighbor hexes (of our selected neighbor) has a pine next to it that has yet to grow
        island.getNeighbors(neighbor)
          .asSequence()
          .filter { it != pieceHex }
          .filter {
            val piece = island.getData(it).piece
            piece is PineTree
          }
          .map { otherPine -> neighbor to otherPine }
      }

  override fun propagate(island: Island, pieceHex: Hexagon<HexagonData>): Boolean {
    require(canGrowThisTurn) { "Palm tree cannot grow this turn" }
    // Find all empty neighbor hexes that are empty
    val candidates = propagateCandidates0(island, pieceHex).toSet()
    Gdx.app.trace("PineTree") {
      candidates.joinToString(prefix = "Pine tree at ${pieceHex.coordinates} can propagate to the following candidates:\n\t", separator = "\n\t") { (neighbor, otherPine) ->
        val otherPinePiece = island.getData(otherPine).piece as PineTree
        "Neighbor ${neighbor.coordinates} together with the other pine @ ${otherPine.coordinates} ($otherPinePiece)"
      }
    }
    val (neighbor, otherPine) = candidates.randomOrNull() ?: return false
    Gdx.app.trace("PineTree") {
      val otherPinePiece = island.getData(otherPine).piece as PineTree
      "Replacing ${neighbor.coordinates} with a pine. together with the other pine @ ${otherPine.coordinates} ($otherPinePiece)"
    }

    // Grow a tree between this pine and another pine
    val placed = replaceWithTree(island, neighbor)
    if (placed) {
      val otherPinePiece = island.getData(otherPine).piece as PineTree
      otherPinePiece.markAsGrown()
      return true
    }
    return false
  }

  override fun copyTo(newData: HexagonData): PineTree = PineTree(newData, placed, lastGrownTurn)
}

class PalmTree(data: HexagonData, placed: Boolean = false, lastGrownTurn: Int = currentTurn()) : TreePiece(data, placed, lastGrownTurn) {

  // Find all empty neighbor hexes that are empty along the cost
  override fun propagateCandidates(island: Island, pieceHex: Hexagon<HexagonData>): Sequence<Hexagon<HexagonData>> =
    island.getNeighbors(pieceHex).asSequence().filter { island.getData(it).piece is Empty && island.isIdealTreeType<PalmTree>(it) }

  override fun propagate(island: Island, pieceHex: Hexagon<HexagonData>): Boolean {
    require(canGrowThisTurn) { "Palm tree cannot grow this turn" }
    val neighbour = propagateCandidates(island, pieceHex).toSet().randomOrNull() ?: return false
    replaceWithTree(island, neighbour)
    return true
  }

  override fun copyTo(newData: HexagonData): PalmTree = PalmTree(newData, placed, lastGrownTurn)
}

// //////////
// LIVING //
// //////////

sealed class LivingPiece(final override val data: HexagonData, var moved: Boolean) : Piece() {

  final override val movable: Boolean = true

  override val canBePlacedOn: Array<KClass<out Piece>> = PLACEABLE_CLASSES

  fun kill(island: Island, hex: Hexagon<HexagonData>) {
    Gdx.app.trace("Piece") { "Replacing piece ${data.piece} (${hex.gridX},${hex.gridZ}) with a grave" }
    island.getData(hex).setPiece<Grave>()
  }

  override fun beginTeamTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    if (pieceHex.isNotPartOfATerritory(island)) {
      kill(island, pieceHex)
    } else {
      moved = false
    }
  }

  fun canNotMerge(with: LivingPiece): Boolean = this.strength + with.strength > BARON_STRENGTH

  fun canMerge(with: LivingPiece): Boolean = !canNotMerge(with)

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
      Gdx.app.trace(DESER_TAG) { "Updating moved of $this to $serializationData" }
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

    /**
     * Sorted list of all subclasses of [LivingPiece] sorted by strength
     */
    val subclassedSortedByStrength: List<KClass<out LivingPiece>> = LivingPiece::class.sealedSubclasses.sortedBy { it.createHandInstance().strength }
  }
}

class Peasant(data: HexagonData, moved: Boolean = true) : LivingPiece(data, moved) {
  override val strength = PEASANT_STRENGTH
  override val income: Int = PEASANT_UPKEEP_COST + 1 // -1
  override val capitalReplacementResistance: Int = 4
  override val price: Int = PEASANT_PRICE
  override fun copyTo(newData: HexagonData): Peasant = Peasant(newData, moved)
}

class Spearman(data: HexagonData, moved: Boolean = true) : LivingPiece(data, moved) {
  override val strength = SPEARMAN_STRENGTH
  override val income: Int = (PEASANT_UPKEEP_COST * 3) + 1 // -5
  override val capitalReplacementResistance: Int = 3
  override val price: Int = 2 * PEASANT_PRICE
  override fun copyTo(newData: HexagonData): Spearman = Spearman(newData, moved)
}

class Knight(data: HexagonData, moved: Boolean = true) : LivingPiece(data, moved) {
  override val strength = KNIGHT_STRENGTH
  override val income: Int = (PEASANT_UPKEEP_COST * 3 * 3) + 1 // -17
  override val capitalReplacementResistance: Int = 2
  override val price: Int = 3 * PEASANT_PRICE
  override fun copyTo(newData: HexagonData): Knight = Knight(newData, moved)
}

class Baron(data: HexagonData, moved: Boolean = true) : LivingPiece(data, moved) {
  override val strength = BARON_STRENGTH
  override val income: Int = (PEASANT_UPKEEP_COST * 3 * 3 * 3) + 1 // -53
  override val capitalReplacementResistance: Int = 1
  override val price: Int = 4 * PEASANT_PRICE
  override fun copyTo(newData: HexagonData): Baron = Baron(newData, moved)
}