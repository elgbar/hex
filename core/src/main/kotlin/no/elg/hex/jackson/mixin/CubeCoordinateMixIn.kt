package no.elg.hex.jackson.mixin

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Elg
 */
interface CubeCoordinateMixIn {

  @get:JsonProperty("x", required = true, index = 0)
  var gridX: Int

  @get:JsonProperty("z", required = true, index = 1)
  var gridZ: Int

  @get:JsonIgnore
  var gridY: Int
}
