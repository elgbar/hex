package no.elg.hex.jackson.mixin

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import no.elg.hex.hexagon.HexagonData
import org.hexworks.cobalt.datatypes.Maybe
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.Point
import org.hexworks.mixite.core.api.Rectangle
import org.hexworks.mixite.core.api.contract.HexagonDataStorage
import org.hexworks.mixite.core.internal.GridData

/**
 * @author Elg
 */
abstract class HexagonMixIn(

  @get:JsonProperty(CUBE_COORDINATES_PROP, index = 0)
  override val cubeCoordinate: CubeCoordinate,

  @get:JsonProperty(index = 2)
  val sharedData: GridData,

  @get:JsonProperty(DATA_PROP, index = 3)
  val hexagonDataStorage: HexagonDataStorage<HexagonData>
) : Hexagon<HexagonData> {

  companion object {
    const val CUBE_COORDINATES_PROP = "cube_coordinates"
    const val DATA_PROP = "data"
  }

  @get:JsonProperty(DATA_PROP, index = 1)
  abstract override val satelliteData: Maybe<HexagonData>

  @JsonSetter(DATA_PROP)
  abstract override fun setSatelliteData(data: HexagonData)

  @get:JsonIgnore
  abstract override val gridX: Int

  @get:JsonIgnore
  abstract override val gridY: Int

  @get:JsonIgnore
  abstract override val gridZ: Int

  @get:JsonIgnore
  abstract override val vertices: List<Double>

  @get:JsonIgnore
  abstract override val center: Point

  @get:JsonIgnore
  abstract override val centerX: Double

  @get:JsonIgnore
  abstract override val centerY: Double

  @get:JsonIgnore
  abstract override val externalBoundingBox: Rectangle

  @get:JsonIgnore
  abstract override val id: String

  @get:JsonIgnore
  abstract override val internalBoundingBox: Rectangle

  @get:JsonIgnore
  abstract override val points: List<Point>

  @JsonIgnore
  abstract override fun clearSatelliteData()

}
