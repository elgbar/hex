package no.elg.hex.input

import no.elg.hex.Hex
import no.elg.hex.input.editor.OpaquenessEditor
import no.elg.hex.util.getData
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * @author Elg
 */
class OpaquenessEditorTest {

  val hexagons = Hex.island.grid.hexagons

  @Test
  internal fun `Add EditMode makes hexagon not opaque`() {
    val hex = hexagons.first()
    val data = hex.getData()

    data.isOpaque = true
    OpaquenessEditor.`Set transparent`.edit(hex)
    assertFalse(data.isOpaque)

    OpaquenessEditor.`Set transparent`.edit(hex)
    assertFalse(data.isOpaque)
  }

  @Test
  internal fun `Delete EditMode makes hexagon opaque`() {
    val hex = hexagons.first()
    val data = hex.getData()

    data.isOpaque = false
    OpaquenessEditor.Delete.edit(hex)
    assertTrue(data.isOpaque)

    OpaquenessEditor.Delete.edit(hex)
    assertTrue(data.isOpaque)
  }

  @Test
  internal fun `Or EditMode makes hexagon opaqueness toggle`() {
    val hex = hexagons.first()
    val data = hex.getData()

    data.isOpaque = false
    OpaquenessEditor.`Disabled`.edit(hex)
    assertTrue(data.isOpaque)

    OpaquenessEditor.`Disabled`.edit(hex)
    assertFalse(data.isOpaque)
  }

}
