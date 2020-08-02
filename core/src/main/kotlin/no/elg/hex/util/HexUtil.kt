package no.elg.hex.util

import no.elg.hex.ApplicationArgumentsParser
import no.elg.hex.Hex
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.HexagonData.Companion.EDGE_DATA
import no.elg.hex.hexagon.HexagonData.Companion.isEdgeHexagon
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.TreePiece
import no.elg.hex.island.Island
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import java.util.HashSet
import kotlin.reflect.KClass

/**
 * @return HexagonData of this hexagon
 */
fun Hexagon<HexagonData>.getData(island: Island): HexagonData {
  return satelliteData.orElseGet {
    (if (isEdgeHexagon(this, island)) EDGE_DATA else HexagonData()).also {
      this.setSatelliteData(it)
    }
  }
}

/**
 * @param x screen x
 * @param y screen y
 *
 * Note that if the application is in map editor mode (via [ApplicationArgumentsParser.mapEditor]) this will return
 * opaque hexagons. If not however these will be invisible to this method.
 *
 * @return Get the hexagon at a given screen location or `null` if nothing is found.
 */
fun Island.getHexagon(x: Double, y: Double): Hexagon<HexagonData>? {
  return this.grid.getByPixelCoordinate(x, y).let {
    if (it.isEmpty()) return@let null
    val data = it.get().getData(this)
    if (data.edge || (!Hex.args.mapEditor && data.isOpaque)) null else it.get()
  }
}

/**
 * @return All visible hexagons connected to the start hexagon of the same team
 */
fun Island.connectedHexagons(hexagon: Hexagon<HexagonData>): Set<Hexagon<HexagonData>> {
  return connectedHexagons(hexagon, hexagon.getData(this).team, HashSet(), this)
}

private fun connectedHexagons(
  center: Hexagon<HexagonData>,
  team: Team,
  visited: MutableSet<Hexagon<HexagonData>>,
  island: Island
): Set<Hexagon<HexagonData>> {
  val data = center.getData(island)
  //only check a hexagon if they have the same color and haven't been visited
  if (visited.contains(center) || data.team != team || data.invisible) {
    return visited
  }

  //add as visited
  visited.add(center)

  //check each neighbor
  for (neighbor in island.grid.getNeighborsOf(center)) {
    connectedHexagons(neighbor, team, visited, island)
  }
  return visited
}

fun Hexagon<HexagonData>.getNeighbors(island: Island, onlyVisible: Boolean = true) =
  island.grid.getNeighborsOf(this).let {
    if (onlyVisible) {
      it.filter { hex -> !hex.getData(island).invisible }
    } else {
      it
    }
  }

fun Hexagon<HexagonData>.treeType(island: Island): KClass<out TreePiece> {
  val neighbors: Collection<Hexagon<HexagonData>> = getNeighbors(island, false)
  return if (neighbors.any { it.getData(island).invisible }) PalmTree::class else PineTree::class
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


fun Hexagon<HexagonData>.findHexagonsWithinRadius(island: Island, radius: Int, includeThis: Boolean = true): Set<Hexagon<HexagonData>> {
  val result = HashSet<Hexagon<HexagonData>>()
  if (includeThis) {
    result += this
  }
  for (subRadius in 1..radius) {
    result += calculateRing(island, subRadius)
  }
  return result
}

fun Hexagon<HexagonData>.calculateRing(island: Island, radius: Int): Set<Hexagon<HexagonData>> {
  val result = HashSet<Hexagon<HexagonData>>()

  var currentCoordinate = CubeCoordinate.fromCoordinates(
    gridX - radius,
    gridZ + radius
  )

  for (i in 0 until 6) {
    for (j in 0 until radius) {
      currentCoordinate = getNeighborCoordinateByIndex(currentCoordinate, i)
      val hexagon = island.grid.getByCubeCoordinate(currentCoordinate)
      if (hexagon.isPresent && !hexagon.get().getData(island).edge) {
        result.add(hexagon.get())
      }
    }
  }

  return result
}
