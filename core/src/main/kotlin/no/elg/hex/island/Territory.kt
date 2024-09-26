package no.elg.hex.island

import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.island.Island.Companion.MIN_HEX_IN_TERRITORY
import no.elg.hex.util.connectedTerritoryHexagons
import no.elg.hex.util.coordinates
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon

/** @author Elg */
data class Territory(
  val island: Island,
  val capital: Capital,
  val hexagons: Collection<Hexagon<HexagonData>>
) {

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
    val enemyHexes = HashSet<Hexagon<HexagonData>>()
    for (hexagon in hexagons) {
      enemyHexes.addAll(
        island.getNeighbors(hexagon).filter { island.getData(it).team != island.currentTeam }
      )
    }
    return@lazy enemyHexes
  }

  val enemyTerritoryHexagons: Collection<Hexagon<HexagonData>> by lazy {
    val enemyHexes = HashSet<Hexagon<HexagonData>>()
    for (hexagon in hexagons) {

      val enemyHex = island.getNeighbors(hexagon).filter { island.getData(it).team != island.currentTeam }.flatMap {
        island.connectedTerritoryHexagons(it)
      }
      enemyHexes.addAll(enemyHex)
    }
    return@lazy enemyHexes
  }

  val team: Team

  val capitalCoordinates: CubeCoordinate

  init {
    require(hexagons.size >= MIN_HEX_IN_TERRITORY) { "Too few hexagons in territory must be at least $MIN_HEX_IN_TERRITORY" }

    var someCapitalCoordinates: CubeCoordinate? = null
    var foundCapital = 0
    team = island.getData(hexagons.first()).team

    for (hexagon in hexagons) {
      val data = island.getData(hexagon)
      if (data.piece === capital) {
        someCapitalCoordinates = hexagon.cubeCoordinate
        foundCapital += 1
      }
      require(data.team == team) {
        "Found a hex that does not have the same team as the rest of the hexagons. " +
          "Expected every team to be on team $team but hex at ${hexagon.coordinates} is on team ${data.team}"
      }
    }

    require(foundCapital == 1) {
      if (foundCapital < 1) {
        "Failed to find the capital among the hexagons in the given hexagons"
      } else {
        "Found $foundCapital capitals! There can only be one capital in a territory"
      }
    }
    requireNotNull(someCapitalCoordinates)
    capitalCoordinates = someCapitalCoordinates
  }

  override fun toString(): String {
    return "Territory of team ${capital.data.team}@${capitalCoordinates.let { "(${it.gridX}, ${it.gridZ})" }}"
  }
}