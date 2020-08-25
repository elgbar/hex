package no.elg.hex.util

fun <E> List<E>.next(current: E): E {
  val nextIndex = (this.indexOf(current) + 1) % this.size
  return this[nextIndex]
}

fun <E> Array<E>.next(current: E): E {
  val nextIndex = (this.indexOf(current) + 1) % this.size
  return this[nextIndex]
}

fun <E> List<E>.previous(current: E): E {
  val currentIndex = this.indexOf(current)
  val nextIndex = if (currentIndex == 0) size - 1 else (currentIndex - 1) % this.size
  return this[nextIndex]
}

fun <E> Array<E>.previous(current: E): E {
  val currentIndex = this.indexOf(current)
  val nextIndex = if (currentIndex == 0) size - 1 else (currentIndex - 1) % this.size
  return this[nextIndex]
}

fun <E> List<E>.nextOrNull(current: E): E? = if (isEmpty()) null else this.next(current)

fun <E> Array<E>.nextOrNull(current: E): E? = if (isEmpty()) null else this.next(current)

fun <E> List<E>.previousOrNull(current: E): E? = if (isEmpty()) null else this.previous(current)

fun <E> Array<E>.previousOrNull(current: E): E? = if (isEmpty()) null else this.previous(current)
