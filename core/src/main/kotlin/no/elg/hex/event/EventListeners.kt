package no.elg.hex.event

import com.badlogic.gdx.utils.Array
import no.elg.hex.event.events.Event

open class EventListeners<T : Event>(val listeners: Array<(T) -> Unit> = Array<(T) -> Unit>())

@Retention(AnnotationRetention.RUNTIME)
annotation class ClearOnScreenChange