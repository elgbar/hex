package no.elg.hex.hexagon

import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Hex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** @author Elg */
internal class HexagonDataTest {
  @Test
  internal fun `Can serialize hexagon data with capital piece`() {
    val hex = HexagonData(false).also { it.setPiece<Capital>() }
    val json = Hex.mapper.writeValueAsString(hex)
    assertEquals(hex, Hex.mapper.readValue<HexagonData>(json))
  }
}