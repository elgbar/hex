package no.elg.hex.event.events

import no.elg.hex.event.EventListeners
import no.elg.hex.util.delegate.PreferenceDelegate

data class SettingsChangeEvent<T : Any>(val delegate: PreferenceDelegate<T>, val old: T, val new: T) : Event {
  companion object : EventListeners<SettingsChangeEvent<*>>()
}