package no.elg.hex.input

import no.elg.hex.Hex
import no.elg.hex.util.getData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * @author Elg
 */
class EditModeTest {

  val hexagons = Hex.island.grid.hexagons

  @Test
  internal fun `Add EditMode makes hexagon not opaque`() {
    val hex = hexagons.first()
    val data = hex.getData()

    data.isOpaque = true
    EditMode.Add.edit(hex)
    assertFalse(data.isOpaque)

    EditMode.Add.edit(hex)
    assertFalse(data.isOpaque)
  }

  @Test
  internal fun `Delete EditMode makes hexagon opaque`() {
    val hex = hexagons.first()
    val data = hex.getData()

    data.isOpaque = false
    EditMode.Delete.edit(hex)
    assertTrue(data.isOpaque)

    EditMode.Delete.edit(hex)
    assertTrue(data.isOpaque)
  }

  @Test
  internal fun `Or EditMode makes hexagon opaqueness toggle`() {
    val hex = hexagons.first()
    val data = hex.getData()

    data.isOpaque = false
    EditMode.Or.edit(hex)
    assertTrue(data.isOpaque)

    EditMode.Or.edit(hex)
    assertFalse(data.isOpaque)
  }

}
