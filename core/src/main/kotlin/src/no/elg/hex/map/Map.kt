package src.no.elg.hex.map

import org.hexworks.mixite.core.api.HexagonOrientation.FLAT_TOP
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridCalculator
import org.hexworks.mixite.core.api.HexagonalGridLayout.RECTANGULAR
import src.no.elg.hex.hexagon.HexagonData


/**
 * @author Elg
 */
class Map(width: Int, height: Int) {

  val grid: HexagonalGrid<HexagonData>
  val calc: HexagonalGridCalculator<HexagonData>

  init {
    val builder = HexagonalGridBuilder<HexagonData>()
      .setGridWidth(width)
      .setGridHeight(height)
      .setGridLayout(RECTANGULAR)
      .setOrientation(FLAT_TOP)
      .setRadius(40.0)


    grid = builder.build()
    calc = builder.buildCalculatorFor(grid)
  }

}
