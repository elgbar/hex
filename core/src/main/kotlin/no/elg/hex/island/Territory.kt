package no.elg.hex.island

import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.HexagonData
import org.hexworks.mixite.core.api.Hexagon

/**
 * @author Elg
 */
data class Territory(val island: Island, val capital: Capital, val hexagons: Collection<Hexagon<HexagonData>>) {

  val income: Int by lazy { capital.calculateIncome(hexagons, island) }
}
