package no.elg.hex.island

import com.fasterxml.jackson.annotation.JsonValue
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.HexagonOrientation.FLAT_TOP
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout


/**
 * @author Elg
 */
class Island(width: Int, height: Int, layout: HexagonalGridLayout, hexagonData: Set<Pair<CubeCoordinate, HexagonData>> = emptySet()) {

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

    if (hexagonData.isNotEmpty()) {
      for ((coord, data) in hexagonData) {
        grid.getByCubeCoordinate(coord).ifPresent {
          it.setSatelliteData(data)
        }
      }
    }
  }


  @get:JsonValue
  private val dto
    get() =
      IslandDTO(
        grid.gridData.gridWidth,
        grid.gridData.gridHeight,
        grid.gridData.gridLayout,
        grid.hexagons.mapTo(HashSet()) { it.cubeCoordinate to it.getData() }
      )

  private data class IslandDTO(val width: Int, val height: Int, val layout: HexagonalGridLayout, val hexagonData: Set<Pair<CubeCoordinate, HexagonData>>)
}
