package no.elg.hex.util

import no.elg.hex.island.Island

data class GridSize(
  val maxX: Double,
  val minX: Double,
  val maxY: Double,
  val minY: Double,
  val maxInvX: Double,
  val maxInvY: Double
) {

  companion object {
    private val EMPTY = GridSize(.0, .0, .0, .0, .0, .0)

    fun Island.calculateGridSize(): GridSize {
      val visible = visibleHexagons
      if (visible.isEmpty()) return EMPTY

      val minX = visible.minOf { it.externalBoundingBox.x }
      val maxX = visible.maxOf { it.externalBoundingBox.x + it.externalBoundingBox.width }

      val minY = visible.minOf { it.externalBoundingBox.y + it.externalBoundingBox.height }
      val maxY = visible.maxOf { it.externalBoundingBox.y }

      val maxInvX = allHexagons.maxOf { it.externalBoundingBox.x + it.externalBoundingBox.width }
      val maxInvY = allHexagons.maxOf { it.externalBoundingBox.y }

      return GridSize(maxX, minX, maxY, minY, maxInvX, maxInvY)
    }
  }
}