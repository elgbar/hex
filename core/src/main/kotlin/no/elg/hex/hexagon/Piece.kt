package no.elg.hex.hexagon

import com.badlogic.gdx.Gdx
import no.elg.hex.island.Island
import no.elg.hex.util.getData
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

}

val PIECES: List<KClass<out Piece>> by lazy {
  val subclasses = ArrayList<KClass<out Piece>>()

  Piece::class.sealedSubclasses.forEach {
    if (it.isSealed) {
      subclasses.addAll(it.sealedSubclasses)
    } else {
      subclasses += it
    }
  }

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

  override fun place(onto: HexagonData): Boolean {
    require(!placed) { "Stationary pieces can only be placed once" }
    if (onto.team != team) {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}", "Stationary pieces can only be placed on a tile with the same team")
      return false
    } else if (onto.piece != Empty) {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}", "Pieces can only be placed on an empty hex")
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

  var alive: Boolean = true
    private set

  fun kill() {
    alive = false
  }

  fun move(to: HexagonData) {
    TODO()
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

  fun updateAnimationTime() {
    if (moved) {
      elapsedAnimationTime = 0f
    } else {
      elapsedAnimationTime += Gdx.graphics.deltaTime
    }
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

  fun calculateIncome(hexagons: Iterable<Hexagon<HexagonData>>, island: Island) = hexagons.sumBy { it.getData(island).piece.cost }

  override val strength = PEASANT_STRENGTH
}

class Castle(team: Team) : StationaryPiece(team) {
  override val strength = SPEARMAN_STRENGTH
  override val cost: Int = 1
}

class PineTree(team: Team) : StationaryPiece(team) {
  override val capitalPlacement = CapitalPlacementPreference.WEAKLY
  override val strength = SPEARMAN_STRENGTH
  override val cost: Int = 0
}

class PalmTree(team: Team) : StationaryPiece(team) {
  override val capitalPlacement = CapitalPlacementPreference.WEAKLY
  override val strength = SPEARMAN_STRENGTH
  override val cost: Int = 0
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
