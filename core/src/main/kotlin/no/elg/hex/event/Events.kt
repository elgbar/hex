package no.elg.hex.event

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array
import kotlinx.coroutines.runBlocking
import ktx.async.onRenderingThread
import no.elg.hex.event.events.Event
import no.elg.hex.util.trace
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.hasAnnotation

/**
 * @author Elg
 */
object Events {

  /**
   * Removes all listeners from all events, if [screenChange] is true then only events that are marked with [ClearOnScreenChange] will be cleared
   *
   * @param screenChange If this is called because the screen is changing
   *
   */
  fun clear(screenChange: Boolean) {
    fun internalClear(clazz: KClass<out Event>, clearChildren: Boolean) {
      for (subclass in clazz.sealedSubclasses) {
        val clear = clearChildren || subclass.hasAnnotation<ClearOnScreenChange>()
        if (subclass.sealedSubclasses.isNotEmpty()) {
          Gdx.app.trace("Event") { "Clearing children of $subclass (with clearChildren=$clear)" }
          internalClear(subclass, clear)
        } else if (clear) {
          Gdx.app.trace("Event") { "Clearing $subclass" }
          getEventList(subclass).clear()
        } else {
          Gdx.app.trace("Event") { "Not clearing $subclass" }
        }
      }
    }

    internalClear(Event::class, !screenChange)
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
            throw IllegalStateException("Registered function is not of correct type. Expected a function '(${event::class.simpleName}) -> Unit' but found a ${listener::class}", e)
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