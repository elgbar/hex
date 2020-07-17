package no.elg.hex.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.elg.hex.Hex
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.jackson.mixin.HexagonMixIn.Companion.CUBE_COORDINATES_PROP
import no.elg.hex.jackson.mixin.HexagonMixIn.Companion.DATA_PROP
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon


/**
 * @author Elg
 */
object HexagonDeserializer : JsonDeserializer<Hexagon<HexagonData>>() {
  override fun deserialize(jsonParser: JsonParser, context: DeserializationContext): Hexagon<HexagonData> {
    val node: JsonNode = jsonParser.codec.readTree(jsonParser)
    val cubeNode = node.get(CUBE_COORDINATES_PROP)
      ?: error("Failed to find cube coordinate field (named $CUBE_COORDINATES_PROP)")
    val cubeCoordinate: CubeCoordinate = Hex.mapper.treeToValue(cubeNode)
      ?: error("Failed to find cube coordinate returned null")

    val hexagon = Hex.map.grid.getByCubeCoordinate(cubeCoordinate).get()

    val dataNode = node.get(DATA_PROP) ?: error("Failed to find data field (named $DATA_PROP)")

    val data: HexagonData = Hex.mapper.treeToValue(dataNode) ?: error("Failed to read hexagon data")
    hexagon.setSatelliteData(data)

    return hexagon
  }
}
