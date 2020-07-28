package no.elg.hex.jackson

import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Hex
import org.hexworks.mixite.core.api.CubeCoordinate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Elg
 */
class CubeCoordinateMixInTest {

  @Test
  fun `can serialize CubeCoordinate`() {
    val cc = CubeCoordinate.fromCoordinates(1, 2)
    assertDoesNotThrow { Hex.mapper.writeValueAsString(cc) }
  }

  @Test
  fun `can deserialize CubeCoordinate`() {
    val cc = CubeCoordinate.fromCoordinates(1, 2)
    val json = Hex.mapper.writeValueAsString(cc)
    assertEquals(cc, Hex.mapper.readValue<CubeCoordinate>(json))
  }

  @Test
  fun `can serialize CubeCoordinate as map key`() {
    val cc = CubeCoordinate.fromCoordinates(1, 2)
    assertDoesNotThrow { Hex.mapper.writeValueAsString(mapOf(cc to 1)) }
  }

  @Test
  fun `can deserialize CubeCoordinate as map key`() {
    val cc = CubeCoordinate.fromCoordinates(1, 2)
    val map = mapOf(cc to 1)
    val json = Hex.mapper.writeValueAsString(map)
    assertEquals(map, Hex.mapper.readValue<Map<CubeCoordinate, Int>>(json))
  }

}
