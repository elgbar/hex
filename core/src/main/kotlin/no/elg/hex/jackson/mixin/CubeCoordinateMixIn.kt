package no.elg.hex.jackson.mixin

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import org.hexworks.mixite.core.api.CubeCoordinate

/**
 * @author Elg
 */
abstract class CubeCoordinateMixIn {

  @JsonValue
  abstract fun toAxialKey(): String

  @get:JsonIgnore
  abstract var gridX: Int

  @get:JsonIgnore
  abstract var gridZ: Int

  @get:JsonIgnore
  abstract var gridY: Int

  companion object {
    @JsonCreator
    @JvmStatic
    fun fromAxialKey(axialKey: String): CubeCoordinate {
      TODO()
    }
  }
}
