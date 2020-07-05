package src.no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex.world
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonalGrid
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
   * @param grid
   * The grid to get all hexagons from
   *
   * @return Get all the hexagons in a grid
   */
  fun getHexagons(grid: HexagonalGrid<HexagonData>): List<Hexagon<HexagonData>> {
    return ArrayList<Hexagon<HexagonData>>().also { it.addAll(grid.hexagons) }
  }

  /**
   * Get the hexagons in a more simple way
   *
   * @return All hexagons from the default game grid.
   */
  val hexagons: List<Hexagon<HexagonData>>
    get() {
      if (hexes.size == 0 || world.hashCode() != worldHash) {
        println("regenerating the grid | old size " + hexes.size)
        hexes = getHexagons(world.grid)
        worldHash = world.hashCode()
      }
      return hexes
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
