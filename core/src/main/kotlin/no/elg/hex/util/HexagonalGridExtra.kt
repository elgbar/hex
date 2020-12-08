package no.elg.hex.util

import no.elg.hex.hexagon.HexagonData
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonalGrid

/**
 * @author Elg
 */

fun HexagonalGrid<HexagonData>.getByCubeCoordinate(coordinate: CubeCoordinate?): Hexagon<HexagonData>? {
  if (coordinate == null) return null
  val maybe = this.getByCubeCoordinate(coordinate)
  return if (maybe.isPresent) maybe.get() else null
}
