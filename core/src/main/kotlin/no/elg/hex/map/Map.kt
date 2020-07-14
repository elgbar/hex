package no.elg.hex.map

import no.elg.hex.hexagon.HexagonData
import org.hexworks.mixite.core.api.HexagonOrientation.FLAT_TOP
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridCalculator
import org.hexworks.mixite.core.api.HexagonalGridLayout.HEXAGONAL


/**
 * @author Elg
 */
class Map(radius: Int) {

  val grid: HexagonalGrid<HexagonData>
  val calc: HexagonalGridCalculator<HexagonData>

  companion object {
    const val GRID_RADIUS = 20.0
  }

  init {
    val builder = HexagonalGridBuilder<HexagonData>()
      .setGridWidth(radius)
      .setGridHeight(radius)
      .setGridLayout(HEXAGONAL)
      .setOrientation(FLAT_TOP)
      .setRadius(GRID_RADIUS)


    grid = builder.build()
    calc = builder.buildCalculatorFor(grid)
  }

}
