package no.elg.hex.island

import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.island.Island.Companion.MIN_HEX_IN_TERRITORY
import no.elg.hex.util.connectedTerritoryHexagons
import no.elg.hex.util.coordinates
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import org.hexworks.mixite.core.api.Hexagon

/** @author Elg */
data class Territory(val island: Island, val capital: Capital, val hexagons: Collection<Hexagon<HexagonData>>, val capitalHexagon: Hexagon<HexagonData>) {

  val team: Team = capital.data.team

  val income: Int
    get() {
      var income = capital.calculateIncome(hexagons, island)
      val hand = island.hand
      if (hand?.territory === this) {
        // Subtract one from the income to balance out the +1 gained from the empty hexagon the piece should be placed on
        income += hand.piece.income - 1
      }
      return income
    }

  /** All enemy hexes that border this territory */
  val enemyBorderHexes: Collection<Hexagon<HexagonData>> by lazy {
    hexagons
      .asSequence()
      .flatMap { island.getNeighbors(it) }
      .filter { island.getData(it).team != team }
      .toSet()
  }

  /**
   * All hexagons that are in enemy territory that border this territory
   */
  val enemyTerritoryHexagons: Collection<Hexagon<HexagonData>> by lazy {
    enemyBorderHexes.flatMap { island.connectedTerritoryHexagons(it) }
  }

  /**
   * A quick check of the validity of this territory.
   * * The capital hexagon still has *the* capital piece
   */
  fun checkWeaklyValid(): Boolean = capitalHexHasCorrectCapitalPiece()

  /**
   * A complete check of the validity of this territory.
   *
   * * All [hexagons] in this territory is on our [team]
   * * There are no new hexagons in this territory
   * * The capital hexagon still has *the* capital piece (i.e., [checkWeaklyValid])
   */
  fun checkStronglyValid(): Boolean =
    capitalHexHasCorrectCapitalPiece() &&
      onlyFriendlyHexagons() &&
      checkNoBorderingFriendlyHexagons() &&
      checkOnlyOneCapital()

  private fun capitalHexHasCorrectCapitalPiece() = island.getData(capitalHexagon).piece === capital
  private fun onlyFriendlyHexagons() = hexagons.all { island.getData(it).team == team }
  private fun checkOnlyOneCapital() = hexagons.count { island.getData(it).piece is Capital } == 1

  private fun checkNoBorderingFriendlyHexagons() =
    hexagons
      .asSequence()
      .flatMap { island.getNeighbors(it) }
      .filter { it !in hexagons }
      .none { island.getData(it).team == team }

  init {
    // Only need to check these on creation
    require(hexagons.size >= MIN_HEX_IN_TERRITORY) { "Too few hexagons in territory must be at least $MIN_HEX_IN_TERRITORY" }
    require(capitalHexagon in hexagons) { "The capital hexagon must be one of the hexagons" }

    // Make sure the territory is valid when created
    require(capitalHexHasCorrectCapitalPiece()) { "The capital hexagon has *the* capital piece" }
    require(onlyFriendlyHexagons()) { "All hexagons in this territory is on our team" }
    require(checkOnlyOneCapital()) { "There are not only one capital found these ${hexagons.filter { island.getData(it).piece is Capital }.joinToString { it.coordinates } }" }
  }

  override fun toString(): String = "Territory of team ${capital.data.team}@${capitalHexagon.coordinates}"
}