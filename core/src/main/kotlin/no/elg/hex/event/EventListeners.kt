package no.elg.hex.event

import no.elg.hex.event.events.Event
import java.util.concurrent.ConcurrentLinkedQueue

open class EventListeners<T : Event>(val listeners: ConcurrentLinkedQueue<(T) -> Unit> = ConcurrentLinkedQueue())

@Retention(AnnotationRetention.RUNTIME)
annotation class ClearOnScreenChange