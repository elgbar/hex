package no.elg.hex.util

import no.elg.hex.hexagon.HexagonData
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridLayout
import org.hexworks.mixite.core.internal.impl.layoutstrategy.GridLayoutStrategy
import kotlin.reflect.jvm.jvmName

/**
 * @author Elg
 */

fun HexagonalGrid<HexagonData>.getByCubeCoordinate(coordinate: CubeCoordinate?): Hexagon<HexagonData>? {
  if (coordinate == null) return null
  val maybe = this.getByCubeCoordinate(coordinate)
  return if (maybe.isPresent) maybe.get() else null
}

fun GridLayoutStrategy.toEnumValue(): HexagonalGridLayout =
  try {
    HexagonalGridLayout.valueOf(getName())
  } catch (e: IllegalArgumentException) {
    throw IllegalArgumentException(
      "Custom hexagonal grid layout is not supported, only acceptable values are the enum constants of ${HexagonalGridLayout::class.jvmName}",
      e
    )
  }