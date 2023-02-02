package no.elg.hex.util

import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

val KProperty0<*>.isLazyInitialized: Boolean
  get() {
    isAccessible = true
    val delegate = getDelegate()
    require(delegate is Lazy<*>) { "KProperty0 $this is not Lazy, this method cannot be used. Delegate: $delegate" }
    return delegate.isInitialized()
  }