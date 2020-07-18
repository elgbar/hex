package no.elg.hex.island

import no.elg.hex.hexagon.HexagonData
import org.hexworks.mixite.core.api.HexagonOrientation.FLAT_TOP
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout


/**
 * @author Elg
 */
class Island(width: Int, height: Int, layout: HexagonalGridLayout) {

  val grid: HexagonalGrid<HexagonData>

  companion object {
    const val GRID_RADIUS = 20.0
  }

  init {
    val builder = HexagonalGridBuilder<HexagonData>()
      .setGridWidth(width)
      .setGridHeight(height)
      .setGridLayout(layout)
      .setOrientation(FLAT_TOP)
      .setRadius(GRID_RADIUS)

    grid = builder.build()
  }
}
