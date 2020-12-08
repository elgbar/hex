package no.elg.hex.util

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Piece
import kotlin.reflect.KClass
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.primaryConstructor

/** @author Elg */
fun <T : Piece> KClass<out T>.createInstance(data: HexagonData): T {
  val objectInstance = objectInstance
  return (
    if (objectInstance != null) objectInstance else {
      val constructor = primaryConstructor ?: error("No primary constructor found")
      val dataParameter = constructor.findParameterByName("data") ?: error("Failed to find the 'data' parameter")
      constructor.callBy(mapOf(dataParameter to data))
    }
    )
}

fun <T : Piece> KClass<out T>.createHandInstance(): T = createInstance(HexagonData.EDGE_DATA)
