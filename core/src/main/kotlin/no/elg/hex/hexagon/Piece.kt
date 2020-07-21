package no.elg.hex.hexagon

import com.badlogic.gdx.Gdx


const val NO_STRENGTH = 0
const val PEASANT_STRENGTH = 1
const val SPEARMAN_STRENGTH = 2
const val KNIGHT_STRENGTH = 3
const val BARON_STRENGTH = 4

enum class CapitalPlacementPreference {
  /**
   * Will be preferred over any other choice
   *
   * @see NoPiece
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
//  val highestPref = hexagons.mapTo(HashSet()) { it.getData().piece.capitalPlacement }
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

  open val capitalPlacement: CapitalPlacementPreference = CapitalPlacementPreference.LAST_RESORT

  /**
   * Action called when this is placed on an hexagon
   *
   * @param onto The hexagon this is placed onto
   *
   * @return If the placement was successful
   */
  abstract fun place(onto: HexagonData): Boolean

  init {
    require(strength >= NO_STRENGTH)
  }

  override fun toString(): String = this::class.simpleName!!
}

object NoPiece : Piece() {
  override val strength: Int = NO_STRENGTH
  override val movable: Boolean = false
  override val team: Team get() = error("Empty Piece is not confined to a team")
  override val capitalPlacement = CapitalPlacementPreference.STRONGLY
  override fun place(onto: HexagonData): Boolean {
    error("Cannot place a 'Nothing' piece, it is meant to represent an empty hexagon!")
  }
}

abstract class StationaryPiece(override val team: Team) : Piece() {

  private var placed: Boolean = false

  final override val movable: Boolean = false

  override fun place(onto: HexagonData): Boolean {
    require(!placed) { "Stationary pieces can only be placed once" }
    if (onto.team != team) {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}", "Stationary pieces can only be placed on a tile with the same team")
      return false
    } else if (onto.piece != NoPiece) {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}", "Pieces can only be placed on an empty hex")
      return false
    }
    onto.piece = this
    placed = true
    return true
  }
}

abstract class DynamicPiece(override val team: Team) : Piece() {

  final override val movable: Boolean = true

  var moved: Boolean = false

  fun move(to: HexagonData) {
    TODO()
  }

  override fun place(onto: HexagonData): Boolean {
    if (onto.piece != NoPiece) {
      Gdx.app.debug("${this::class.simpleName}-${this::place.name}", "Pieces can only be placed on an empty hex")
      return false
    } else if (onto.team == team) {
      Gdx.app.log("${this::class.simpleName}-${this::place.name}", "Dynamic pieces can only be placed on a tile with the same team")
      return false
    }
    return true
  }
}

class Capital(team: Team) : StationaryPiece(team) {

  /**
   * How much money this territory currently has
   */
  var balance: Int = 0

  /**
   * Transfer everything to the given capital
   */
  fun transfer(to: Capital) {
    to.balance += balance
    balance = 0
  }


  override val strength = PEASANT_STRENGTH
}

class Castle(team: Team) : StationaryPiece(team) {
  override val strength = SPEARMAN_STRENGTH
}

class PineTree(team: Team) : StationaryPiece(team) {
  override val capitalPlacement = CapitalPlacementPreference.WEAKLY
  override val strength = SPEARMAN_STRENGTH
}

class PalmTree(team: Team) : StationaryPiece(team) {
  override val capitalPlacement = CapitalPlacementPreference.WEAKLY
  override val strength = SPEARMAN_STRENGTH
}

class Peasant(team: Team) : DynamicPiece(team) {
  override val strength = PEASANT_STRENGTH
}

class Spearman(team: Team) : DynamicPiece(team) {
  override val strength = SPEARMAN_STRENGTH
}

class Knight(team: Team) : DynamicPiece(team) {
  override val strength = KNIGHT_STRENGTH
}

class Baron(team: Team) : DynamicPiece(team) {
  override val strength = BARON_STRENGTH
}
