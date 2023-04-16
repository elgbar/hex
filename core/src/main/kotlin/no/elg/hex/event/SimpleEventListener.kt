package no.elg.hex.event

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.event.SimpleEventListener.Companion.create
import kotlin.reflect.KClass

/**
 * Simplify event listening by automatically removing the event by calling [dispose] on this object
 *
 * Call [create] for the best experience
 *
 */
class SimpleEventListener<T : Event>(clazz: KClass<T>, private val run: (T) -> Unit) : Disposable {

  @Suppress("UNCHECKED_CAST")
  private val eventList = (Events.getEventList(clazz) as Array<(T) -> Unit>)

  init {
    require(!clazz.isAbstract) { "Cannot create a listener for an abstract event: $clazz" }
    eventList.add(run)
  }

  override fun dispose() {
    eventList.removeValue(run, true)
  }

  companion object {
    inline fun <reified T : Event> create(noinline run: (T) -> Unit) = SimpleEventListener(T::class, run)
  }
}