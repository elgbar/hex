package src.no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex.map
import org.hexworks.mixite.core.api.Hexagon
import java.util.HashSet

/**
 * @author kheba
 */
object HexUtil {

  /**
   * @param this@getData
   * The hexagon to get the data from
   *
   * @return HexagonData of this hexagon
   */
  fun Hexagon<HexagonData>.getData(): HexagonData {
    return satelliteData.orElseGet {
      HexagonData().also {
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
    return map.grid.getByPixelCoordinate(x, y).let { if (it.isPresent) it.get() else null }
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
    for (neighbor in map.grid.getNeighborsOf(center)) {
      connectedHexagons(neighbor, color, visited)
    }
    return visited
  }
}
