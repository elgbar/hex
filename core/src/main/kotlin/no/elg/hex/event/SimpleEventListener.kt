package no.elg.hex.event

import com.badlogic.gdx.utils.Disposable
import no.elg.hex.event.SimpleEventListener.Companion.create
import no.elg.hex.event.events.Event
import kotlin.reflect.KClass

/**
 * Simplify event listening by automatically removing the event by calling [dispose] on this object
 *
 * Call [create] for the best experience
 *
 */
class SimpleEventListener<T : Event>(clazz: KClass<T>, private val run: (T) -> Unit) : Disposable {

  private val eventList = Events.getEventList(clazz)

  init {
    require(!clazz.isAbstract) { "Cannot create a listener for an abstract event: $clazz" }
    eventList += run
  }

  override fun dispose() {
    eventList -= run
  }

  companion object {
    inline fun <reified T : Event> create(noinline run: (T) -> Unit) = SimpleEventListener(T::class, run)
  }
}