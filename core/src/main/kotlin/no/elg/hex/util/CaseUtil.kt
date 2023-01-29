package no.elg.hex.util

val CAMELCASE_REGEX = "([A-Z][a-z]|[A-Z]+(?![a-z]))".toRegex()

fun CharSequence.toTitleCase(): String {
  val title = CAMELCASE_REGEX.replace(this) { " ${it.value}" }
  val first = title.first()
  return "${if (first.isWhitespace()) "" else first.uppercaseChar()}${title.drop(1)}"
}
