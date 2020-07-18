package no.elg.hex.util

import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex.island
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.HexagonData.Companion.isEdgeHexagon
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import java.util.HashSet

/**
 * @author kheba
 */

/**
 * @param this@getData
 * The hexagon to get the data from
 *
 * @return HexagonData of this hexagon
 */
fun Hexagon<HexagonData>.getData(): HexagonData {
  return satelliteData.orElseGet {
    HexagonData(edge = isEdgeHexagon(this)).also {
      setSatelliteData(it)
    }
  }
}

/**
 * @param x
 * screen x
 * @param y
 * screen y
 *
 * @return Get the hexagon at a given screen location or `null` if nothing is found
 */
fun getHexagon(x: Double, y: Double): Hexagon<HexagonData>? {
  return island.grid.getByPixelCoordinate(x, y).let { if (it.isPresent && !it.get().getData().edge) it.get() else null }
}


/**
 * @param initial The hexagon to start at
 *
 * @return All hexagons connected to the start hexagon that has the same color
 */
fun connectedHexagons(initial: Hexagon<HexagonData>): Set<Hexagon<HexagonData>?> {
  return connectedHexagons(initial, initial.getData().color, HashSet())
}

private fun connectedHexagons(
  center: Hexagon<HexagonData>,
  color: Color,
  visited: MutableSet<Hexagon<HexagonData>?>
): Set<Hexagon<HexagonData>?> {
  //only check a hexagon if they have the same color and haven't been visited
  if (visited.contains(center) || center.getData().color != color) {
    return visited
  }

  //add as visited
  visited.add(center)

  //check each neighbor
  for (neighbor in island.grid.getNeighborsOf(center)) {
    connectedHexagons(neighbor, color, visited)
  }
  return visited
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Taken from https://github.com/Hexworks/mixite/pull/56 TODO remove when this is in the library                     //
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

const val NEIGHBOR_X_INDEX = 0
const val NEIGHBOR_Z_INDEX = 1
val NEIGHBORS = arrayOf(intArrayOf(+1, 0), intArrayOf(+1, -1), intArrayOf(0, -1), intArrayOf(-1, 0), intArrayOf(-1, +1), intArrayOf(0, +1))


fun getNeighborCoordinateByIndex(coordinate: CubeCoordinate, index: Int) =
  CubeCoordinate.fromCoordinates(
    coordinate.gridX + NEIGHBORS[index][NEIGHBOR_X_INDEX],
    coordinate.gridZ + NEIGHBORS[index][NEIGHBOR_Z_INDEX]
  )


fun Hexagon<HexagonData>.findHexagonsWithinRadius(radius: Int, includeThis: Boolean = true): Set<Hexagon<HexagonData>> {
  val result = HashSet<Hexagon<HexagonData>>()
  if (includeThis) {
    result += this
  }
  for (subRadius in 1..radius) {
    result += calculateRing(subRadius)
  }
  return result
}

fun Hexagon<HexagonData>.calculateRing(radius: Int): Set<Hexagon<HexagonData>> {
  val result = HashSet<Hexagon<HexagonData>>()

  var currentCoordinate = CubeCoordinate.fromCoordinates(
    gridX - radius,
    gridZ + radius
  )


  for (i in 0 until 6) {
    for (j in 0 until radius) {
      currentCoordinate = getNeighborCoordinateByIndex(currentCoordinate, i)
      val hexagon = island.grid.getByCubeCoordinate(currentCoordinate)
      if (hexagon.isPresent && !hexagon.get().getData().edge) {
        result.add(hexagon.get())
      }
    }
  }

  return result
}
