package no.elg.hex.input

import no.elg.hex.island.Island
import org.hexworks.mixite.core.api.HexagonalGridLayout.HEXAGONAL
import org.junit.jupiter.api.Test

/** @author Elg */
class OpaquenessEditorTest {

  var island: Island

  init {
    island = Island(5, 5, HEXAGONAL)
  }

  val hexagons = island.grid.hexagons

  @Test
  internal fun `Set transparent EditMode makes hexagon not opaque`() {
//    val hex = hexagons.first()
//    val data = hex.getData(island)
//
//    data.isOpaque = true
//    OpaquenessEditor.`Set transparent`.edit(hex)
//    assertFalse(data.isOpaque)
//
//    OpaquenessEditor.`Set transparent`.edit(hex)
//    assertFalse(data.isOpaque)
  }

  @Test
  internal fun `Set opaque EditMode makes hexagon opaque`() {
//    val hex = hexagons.first()
//    val data = hex.getData(island)
//
//    data.isOpaque = false
//    OpaquenessEditor.`Set opaque`.edit(hex)
//    assertTrue(data.isOpaque)
//
//    OpaquenessEditor.`Set opaque`.edit(hex)
//    assertTrue(data.isOpaque)
  }

  @Test
  internal fun `Toggle opaqueness EditMode makes hexagon opaqueness toggle`() {
//    val hex = hexagons.first()
//    val data = hex.getData(island)
//
//    data.isOpaque = false
//    OpaquenessEditor.`Toggle opaqueness`.edit(hex)
//    assertTrue(data.isOpaque)
//
//    OpaquenessEditor.`Toggle opaqueness`.edit(hex)
//    assertFalse(data.isOpaque)
  }
}