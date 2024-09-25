package no.elg.hex.util

import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

val KProperty0<*>.isLazyInitialized: Boolean
  get() {
    isAccessible = true
    val delegate = getDelegate()
    require(delegate is Lazy<*>) { "KProperty0 $this is not Lazy, this method cannot be used. Delegate: $delegate" }
    return delegate.isInitialized()
  }

fun KProperty0<*>.safeGetDelegate(): Any? {
  isAccessible = true
  return getDelegate()
}

fun <T> KProperty1<T, *>.safeGetDelegate(reciver: T): Any? {
  isAccessible = true
  return getDelegate(reciver)
}