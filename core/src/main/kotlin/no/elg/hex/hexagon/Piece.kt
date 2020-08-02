package no.elg.hex.hexagon

import com.badlogic.gdx.Gdx
import no.elg.hex.island.Island
import no.elg.hex.util.connectedHexagons
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.treeType
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.KClass


const val NO_STRENGTH = 0
const val PEASANT_STRENGTH = 1
const val SPEARMAN_STRENGTH = 2
const val KNIGHT_STRENGTH = 3
const val BARON_STRENGTH = 4

enum class CapitalPlacementPreference {
  /**
   * Will be preferred over any other choice
   *
   * @see Empty
   */
  STRONGLY,

  /**
   * Not ideal, but better than [LAST_RESORT]
   */
  WEAKLY,

  /**
   * Can only be overwritten as a last resort
   */
  LAST_RESORT
}

///**
// * Only return the pices of the strongest preferences. F.eks if any [STRONGLY] they are always returned. If no [STRONGLY] are presenent but one [LAST_RESORT] and one [WEAKLY] are present only [WEAKLY] will be returned
// */
//fun filterToCapitalPreference(hexagons: Iterable<Hexagon<HexagonData>>): Set<Hexagon<HexagonData>> {
//  val highestPref = hexagons.mapTo(HashSet()) { it.getData(island).piece.capitalPlacement }
//  if(h)
//}

/**
 * @author Elg
 */
sealed class Piece {

  protected abstract val team: Team

  /**
   * How powerful an object is. Must be Strictly positive
   */
  abstract val strength: Int

  /**
   * Can this object move once placed
   */
  abstract val movable: Boolean

  /**
   * How much the territory gain/lose by maintaining this piece
   */
  abstract val cost: Int

  open val capitalPlacement: CapitalPlacementPreference = CapitalPlacementPreference.LAST_RESORT

  /**
   * Action called when this is placed on an hexagon
   *
   * @param onto The hexagon this is placed onto
   *
   * @return If the placement was successful
   */
  abstract fun place(onto: HexagonData): Boolean

  override fun toString(): String = this::class.simpleName!!

  var elapsedAnimationTime = 0f

  /**
   * Called when each team ends it's turn. The [pieceHex], [data], and [team] are guaranteed to be synced.
   * Only hexagons who's turn it is will be called.
   */
  open fun endTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    //NO-OP
  }

  /**
   * Called when all visible hexagons's [endTurn] has been called
   */
  open fun newTurn(island: Island, pieceHex: Hexagon<HexagonData>) {
    //NO-OP
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

object Empty : Piece() {
  override val strength: Int = NO_STRENGTH
  override val movable: Boolean = false
  override val team: Team get() = error("Empty Piece is not confined to a team")
  override val capitalPlacement = CapitalPlacementPreference.STRONGLY
  override val cost: Int = 1
  override fun place(onto: HexagonData): Boolean = true
}

sealed class StationaryPiece(override val team: Team) : Piece() {

  private var placed: Boolean = false

  final override val movable: Boolean = false
  open val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(Empty::class)

  override fun place(onto: HexagonData): Boolean {
    require(!placed) { "Stationary pieces can only be placed once" }
    if (onto.team != team) {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}", "Stationary pieces can only be placed on a tile with the same team")
      return false
    } else if (!canBePlacedOn.any { it.isInstance(onto.piece) }) {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}", "Piece ${this::class.simpleName} can only be placed on ${canBePlacedOn.map { it::simpleName }} pieces")
      return false
    }
    placed = true
    return true
  }

  override fun toString(): String {
    return super.toString() + ": placed? $placed"
  }
}

sealed class LivingPiece(override val team: Team) : Piece() {

  final override val movable: Boolean = true

  var moved: Boolean = false

  fun kill(island: Island, hex: Hexagon<HexagonData>) {
    hex.getData(island).setPiece(Grave::class)
  }

  fun move(island: Island, from: Hexagon<HexagonData>, to: Hexagon<HexagonData>) {
    moved = true
  }

  override fun newTurn(island: Island, pieceHex: Hexagon<HexagonData>) {
    moved = false
  }

  override fun place(onto: HexagonData): Boolean {
    if (onto.piece != Empty) {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}", "Pieces can only be placed on an empty hex")
      return false
    } else if (onto.team != team) {
      Gdx.app.log("${this::class.simpleName}-${this::place.name}", "Dynamic pieces can only be placed on a tile with the same team. This team = $team, onto team = ${onto.team}")
      return false
    }
    return true
  }

  fun updateAnimationTime(): Float {
    if (moved) {
      elapsedAnimationTime = 0f
    } else {
      elapsedAnimationTime += Gdx.graphics.deltaTime
    }
    return elapsedAnimationTime
  }

  override fun toString(): String {
    return super.toString() + ": moved? $moved"
  }
}

class Capital(team: Team) : StationaryPiece(team) {

  /**
   * How much money this territory currently has
   */
  var balance: Int = 0
  override val cost: Int = 1

  /**
   * Transfer everything to the given capital
   */
  fun transfer(to: Capital) {
    to.balance += balance
    balance = 0
  }

  private fun killall(island: Island, iterable: Iterable<Hexagon<HexagonData>>) {
    for (hex in iterable) {
      val piece = hex.getData(island).piece
      if (piece is LivingPiece) {
        piece.kill(island, hex)
      }
    }
  }

  override fun endTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    val hexagons = island.getTerritoryHexagons(pieceHex)

    if (hexagons == null) {
      killall(island, island.connectedHexagons(pieceHex))
      pieceHex.getData(island).setPiece(pieceHex.treeType(island))
      return
    }

    require(island.getCapitalOf(hexagons) === this) { "Mismatch between this piece and the capital of the territory!" }

    balance += calculateIncome(hexagons, island)
    if (balance < 0) {
      killall(island, hexagons)
    }
  }

  fun calculateIncome(hexagons: Iterable<Hexagon<HexagonData>>, island: Island) = hexagons.sumBy { it.getData(island).piece.cost }

  override val strength = PEASANT_STRENGTH
}

class Castle(team: Team) : StationaryPiece(team) {
  override val strength = SPEARMAN_STRENGTH
  override val cost: Int = 1
}

class Grave(team: Team) : StationaryPiece(team) {
  override val strength = NO_STRENGTH
  override val cost: Int = 1

  private var timeToTree = 1f

  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(LivingPiece::class)

  override fun newTurn(island: Island, pieceHex: Hexagon<HexagonData>) {
    if (timeToTree > 0) {
      timeToTree--
      return
    }
    pieceHex.getData(island).setPiece(pieceHex.treeType(island))
  }
}

sealed class TreePiece(team: Team) : StationaryPiece(team) {

  protected var hasGrown: Boolean = true

  override val capitalPlacement = CapitalPlacementPreference.WEAKLY
  override val strength = NO_STRENGTH
  override val cost: Int = 0
  override val canBePlacedOn: Array<KClass<out Piece>> = arrayOf(Empty::class, Capital::class, Grave::class)

  override fun endTurn(island: Island, pieceHex: Hexagon<HexagonData>, data: HexagonData, team: Team) {
    hasGrown = false
  }
}

class PineTree(team: Team) : TreePiece(team) {
  override fun newTurn(island: Island, pieceHex: Hexagon<HexagonData>) {
    if (hasGrown) return

    //Find all empty neighbor hexes that are empty
    val list = pieceHex.getNeighbors(island).filter {
      val piece = it.getData(island).piece
      piece is Empty && it.treeType(island) == PineTree::class
    }.shuffled()

    for (hexagon in list) {
      //Find all neighbor hexes (of our selected neighbor) has a pine next to it that has yet to grow
      val otherPines = hexagon.getNeighbors(island).filter {
        if (it == pieceHex) return@filter false
        val piece = it.getData(island).piece
        return@filter piece is PineTree && !piece.hasGrown && it != pieceHex
      }
      if (otherPines.isNotEmpty()) {
        //Grow a tree between this pine and another pine
        val otherPine = otherPines.random().getData(island).piece as PineTree
        val newPine = hexagon.getData(island).setPiece(PineTree::class)
        if (newPine is PineTree) {
          hasGrown = true
          otherPine.hasGrown = true
          newPine.hasGrown = true
          break
        }
      }
    }
  }
}

class PalmTree(team: Team) : TreePiece(team) {
  @ExperimentalStdlibApi
  override fun newTurn(island: Island, pieceHex: Hexagon<HexagonData>) {
    if (hasGrown) return
    //Find all empty neighbor hexes that are empty along the cost
    val piece = pieceHex.getNeighbors(island).filter {
      val piece = it.getData(island).piece
      piece is Empty && it.treeType(island) == PalmTree::class
    }.randomOrNull()?.getData(island)?.setPiece(PalmTree::class)
    if (piece is PalmTree) {
      piece.hasGrown = true
      hasGrown = true
    }
  }
}

class Peasant(team: Team) : LivingPiece(team) {
  override val strength = PEASANT_STRENGTH
  override val cost: Int = -1
}

class Spearman(team: Team) : LivingPiece(team) {
  override val strength = SPEARMAN_STRENGTH
  override val cost: Int = -5
}

class Knight(team: Team) : LivingPiece(team) {
  override val strength = KNIGHT_STRENGTH
  override val cost: Int = -17
}

class Baron(team: Team) : LivingPiece(team) {
  override val strength = BARON_STRENGTH
  override val cost: Int = -53
}
