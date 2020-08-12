package no.elg.hex.util

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Piece

/** @author Elg */
fun KClass<out Piece>.createInstance(data: HexagonData): Piece {
  return if (objectInstance != null) {
    objectInstance
  } else {
    primaryConstructor?.call(data)
  }
      ?: error("No constructor found with a single ${data::class.simpleName} argument")
}
