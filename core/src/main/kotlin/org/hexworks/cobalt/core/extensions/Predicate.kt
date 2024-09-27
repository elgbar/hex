package org.hexworks.cobalt.core.extensions

/**
 * [source](https://github.com/Hexworks/cobalt/blob/05f37042cbca3bf354ebd777d559701cf920b1bf/cobalt.core/src/commonMain/kotlin/org/hexworks/cobalt/core/extensions/Predicates.kt)
 */

typealias Predicate<T> = Function1<T, Boolean>

infix fun <T> Predicate<T>.and(other: Predicate<T>): Predicate<T> = {
  this(it) && other(it)
}

fun <T> Predicate<T>.negate(): Predicate<T> = {
  !this(it)
}

infix fun <T> Predicate<T>.or(other: Predicate<T>): Predicate<T> = {
  this(it) || other(it)
}