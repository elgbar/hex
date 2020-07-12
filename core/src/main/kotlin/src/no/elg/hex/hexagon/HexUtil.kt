package src.no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex.world
import org.hexworks.mixite.core.api.Hexagon
import java.util.ArrayList
import java.util.HashSet

/**
 * @author kheba
 */
object HexUtil {
  private var hexes: List<Hexagon<HexagonData>> = ArrayList()
  private var worldHash = 0

  /**
   * @param hexagon
   * The hexagon to get the data from
   *
   * @return HexagonData of this hexagon
   */
  fun getData(hexagon: Hexagon<HexagonData>): HexagonData {
    return hexagon.satelliteData.orElseGet {
      HexagonData().also {
        hexagon.setSatelliteData(it)
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
    return world.grid.getByPixelCoordinate(x, y).let { if (it.isPresent) it.get() else null }
  }

  /**
   * @param start
   * The hexagon to start at
   *
   * @return All hexagons with the same color that are connected and their neighbors
   */
  fun connectedAdjacentHexagons(start: Hexagon<HexagonData>): Set<Hexagon<HexagonData>?> {
    return adjacentHexagons(connectedHexagons(start))
  }

  /**
   * @param start
   * The hexagon to start at
   *
   * @return All hexagons connected to the start hexagon that has the same color
   */
  fun connectedHexagons(start: Hexagon<HexagonData>): Set<Hexagon<HexagonData>?> {
    return connectedHexagons(start, getData(start).color, HashSet())
  }

  private fun connectedHexagons(center: Hexagon<HexagonData>, color: Color,
                                visited: MutableSet<Hexagon<HexagonData>?>): Set<Hexagon<HexagonData>?> {
    //only check a hexagon if they have the same color and haven't been visited
    if (visited.contains(center) || getData(center).color != color) {
      return visited
    }

    //add as visited
    visited.add(center)

    //check each neighbor
    for (neighbor in world.grid.getNeighborsOf(center)) {
      connectedHexagons(neighbor, color, visited)
    }
    return visited
  }

  /**
   * @param set
   *
   * @return
   */
  fun adjacentHexagons(set: Collection<Hexagon<HexagonData>?>): Set<Hexagon<HexagonData>?> {
    val adjacent: MutableSet<Hexagon<HexagonData>?> = HashSet()
    adjacent.addAll(set)
    for (hex in set) {
      for (neighbor in world.grid.getNeighborsOf(hex!!)) {
        if (!adjacent.contains(neighbor)) {
          adjacent.add(neighbor)
        }
      }
    }
    return adjacent
  }
}
