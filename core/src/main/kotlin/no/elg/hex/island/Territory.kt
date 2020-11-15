package no.elg.hex.island

import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.island.Island.Companion.MIN_HEX_IN_TERRITORY
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import org.hexworks.mixite.core.api.Hexagon

/** @author Elg */
data class Territory(
  val island: Island,
  val capital: Capital,
  val hexagons: Collection<Hexagon<HexagonData>>
) {

  val income: Int by lazy { capital.calculateIncome(hexagons, island) }

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

  val team: Team

  init {
    require(hexagons.size >= MIN_HEX_IN_TERRITORY) {
      "Too few hexagons in territory must be at least $MIN_HEX_IN_TERRITORY"
    }
    var foundCapital = false
    team = island.getData(hexagons.first()).team
    for (hexagon in hexagons) {
      for (it in hexagons) {
        val data = island.getData(it)
        if (data.piece === capital) foundCapital = true
        require(data.team == team) {
          "Found a hex that does not have the same team as the rest of the hexagons. " +
            "Expected every team to be on team $team but hex at ${it.cubeCoordinate} is on team ${data.team}"
        }
      }
    }
    require(foundCapital) { "Failed to find the capital among the hexagons in the given hexagons" }
  }

  override fun toString(): String {
    return "Territory of team ${capital.data.team}"
  }
}
