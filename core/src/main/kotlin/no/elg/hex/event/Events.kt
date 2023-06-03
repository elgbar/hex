package no.elg.hex.event

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import kotlinx.coroutines.runBlocking
import ktx.async.onRenderingThread
import no.elg.hex.util.trace
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

/**
 * @author Elg
 */
object Events {

  /**
   * Removes all listeners from all events
   */
  fun clear() {
    fun internalClear(clazz: KClass<out Event>) {
      for (subclass in clazz.sealedSubclasses) {
        if (subclass.sealedSubclasses.isNotEmpty()) {
          internalClear(subclass)
        } else {
          getEventList(subclass).clear()
        }
      }
    }
    internalClear(Event::class)
  }

  inline fun <reified T : Event> fireEvent(event: T) {
    val listeners: Array<(T) -> Unit> = getEventList(T::class)

    if (listeners.isEmpty) return
    Gdx.app.trace("Event") { "Firing event $event" }

    runBlocking {
      onRenderingThread {
        for (listener in listeners) {
          try {
            listener(event)
          } catch (e: ClassCastException) {
            error("Registered function is not of correct type. Expected a function '(${event::class.simpleName}) -> Unit' but found a ${listener::class}")
          }
        }
      }
    }
  }

  /**
   * Internal function to get the list of listener for an event
   */
  fun <T : Event> getEventList(eventClass: KClass<T>): Array<(T) -> Unit> {
    val companionInstance = eventClass.companionObjectInstance ?: error("No companion object found for ${eventClass.simpleName}")

    @Suppress("UNCHECKED_CAST")
    val companionInstanceA = companionInstance as? EventListeners<T> ?: error("Companion object of ${eventClass.simpleName} is not an EventListeners<${eventClass.simpleName}>")
    return companionInstanceA.listeners
  }
}