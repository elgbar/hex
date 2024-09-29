package no.elg.hex.event

import no.elg.hex.event.events.Event

open class EventListeners<T : Event>(val listeners: MutableList<(T) -> Unit> = mutableListOf())

@Retention(AnnotationRetention.RUNTIME)
annotation class ClearOnScreenChange