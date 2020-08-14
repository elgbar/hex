package no.elg.hex.util

import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

val KProperty0<*>.isLazyInitialized: Boolean
  get() {
    // Prevent IllegalAccessException from JVM access check
    isAccessible = true
    return (getDelegate() as Lazy<*>).isInitialized()
  }
