package no.elg.hex.jackson.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.PIECES_MAP

/**
 * @author Elg
 */
class HexagonDataDeserializer(defaultSerializer: BeanDeserializer) :
  BeanDeserializer(defaultSerializer),
  ResolvableDeserializer {

  override fun deserialize(parser: JsonParser, context: DeserializationContext): HexagonData {
    val hexagonData = super.deserialize(parser, context) as HexagonData

    with(hexagonData) {
      // special loading required to properly serialize the piece on the hexagon
      // Jacksons default ordering is not reliable enough to allow loading of piece then the data for that piece.
      // A less hacky solution would be to serialize the pieces themself, but this is hard to do (see the #setPiece(class) method)
      if (loadedTypeName != null) {
        val pieceClass = PIECES_MAP[loadedTypeName] ?: error("Unknown piece with the name $loadedTypeName")
        val pieceUpdated = setPiece(pieceClass) {
          this.handleDeserializationData(serializationDataToLoad)
        }
        require(pieceUpdated) { "Failed to set piece to $serializationDataToLoad during serialization" }
      }
    }

    return hexagonData
  }
}

class HexagonDataDeserializerModifier : BeanDeserializerModifier() {

  override fun modifyDeserializer(
    config: DeserializationConfig?,
    beanDesc: BeanDescription,
    deserializer: JsonDeserializer<*>
  ): JsonDeserializer<*> {
    if (beanDesc.beanClass == HexagonData::class.java) {
      require(deserializer is BeanDeserializer) { "Default deserializer must be a BeanDeserializer" }
      return HexagonDataDeserializer(deserializer)
    }
    return super.modifyDeserializer(config, beanDesc, deserializer)
  }
}