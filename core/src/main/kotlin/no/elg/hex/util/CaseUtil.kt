package no.elg.hex.util

val CAMELCASE_REGEX = "([A-Z])".toRegex()

fun CharSequence.toTitleCase(): String {
  val title = CAMELCASE_REGEX.replace(this) { " ${it.value}" }
  val first = title.first()
  return "${if (first.isWhitespace()) "" else first.toUpperCase()}${title.drop(1)}"
}
