package no.elg.hex.util

import kotlin.reflect.KClass

/**
 * @author Elg
 */
inline fun <reified T : Enum<T>> String.toEnum(): T {
  return enumValueOf(this.uppercase())
}

inline fun <T : Enum<T>> String.toEnum(enumClass: KClass<T>): T {
  return requireNotNull(toEnumOrNull(enumClass)) { "No enum with the name '${this.uppercase()}' of type $enumClass" }
}

inline fun <T : Enum<T>> String.toEnumOrNull(enumClass: KClass<T>): T? {
  return enumClass.java.enumConstants.find { it.name.equals(this, ignoreCase = true) }
}

inline fun <T : Enum<T>> findEnumValues(enumClass: KClass<T>): List<T> {
  require(enumClass.java.isEnum) { "Given class is not an enum class" }
  @Suppress("UNCHECKED_CAST")
  return enumClass.java.enumConstants.sortedBy { it.name }
}
