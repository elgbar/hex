package no.elg.hex.hexagon

import com.badlogic.gdx.utils.Disposable
import kotlinx.coroutines.runBlocking
import ktx.async.onRenderingThread
import no.elg.hex.hexagon.HexagonDataEvents.getEventList
import no.elg.hex.hexagon.SimpleEventListener.Companion.create
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmName
import com.badlogic.gdx.utils.Array as GdxUtilsArray

/**
 * @author Elg
 */
object HexagonDataEvents {

  private const val EXPECTED_LISTENERS_FIELD_NAME = "listeners"

  fun onTeamChange(listener: (TeamChangeHexagonDataEvent) -> Unit): (TeamChangeHexagonDataEvent) -> Unit {
    TeamChangeHexagonDataEvent.listeners.add(listener)
    return listener
  }

  inline fun <reified T : HexagonDataEvent> removeListener(noinline listener: (T) -> Unit) {
    (getEventList(T::class) as GdxUtilsArray<(T) -> Unit>).removeValue(listener, true)
  }

  /**
   * Removes all listeners from all events
   */
  fun clear() {
    for (subclass in HexagonDataEvent::class.sealedSubclasses) {
      getEventList(subclass).clear()
    }
  }

  fun <T : HexagonDataEvent> fireEvent(event: T) {
    val listeners = getEventList(event::class)

    if (listeners.isEmpty) return

    runBlocking {
      onRenderingThread {

        for (listener in listeners) {
          requireNotNull(listener) { "Null function given as a listener!" }
          try {
            (listener as Function1<T, Unit>)(event)
          } catch (e: ClassCastException) {
            error("Registered function is not of correct type. Expected a function (${event::class.simpleName} -> Unit) but found a ${listener::class.simpleName}")
          }
        }
      }
    }
  }

  /**
   * Internal function to get the list of listener for an event
   */
  fun getEventList(eventClass: KClass<out HexagonDataEvent>): GdxUtilsArray<*> {
    val companionClass = eventClass.companionObject ?: error("No companion object found for ${eventClass.simpleName}")
    val companionInstance = eventClass.companionObjectInstance ?: error("No companion object found for ${eventClass.simpleName}")
    val listenersField = companionClass.declaredMembers.firstOrNull { it.name == EXPECTED_LISTENERS_FIELD_NAME }
      ?: error("Failed to find a member within ${eventClass.simpleName}s companion object called $EXPECTED_LISTENERS_FIELD_NAME")

    // Check that the return type is GdxUtilsArray (though we still do not know the generic parameter!)
    require(listenersField.returnType.isSubtypeOf(GdxUtilsArray::class.createType(arguments = listOf(STAR)))) {
      "The '$EXPECTED_LISTENERS_FIELD_NAME' member must be an instance of ${GdxUtilsArray::class.jvmName}"
    }

    // The first (and hopefully only) parameter is the instance parameter
    require(listenersField.parameters.size == 1) {
      "The '$EXPECTED_LISTENERS_FIELD_NAME' member cannot have any arguments. Found the following arguments ${listenersField.parameters}"
    }

    return listenersField.call(companionInstance) as GdxUtilsArray<*>
  }
}

sealed class HexagonDataEvent(val data: HexagonData)

/**
 * Called when the team of a [HexagonData] will be changed. Event is called after the change occur
 */
class TeamChangeHexagonDataEvent(data: HexagonData, val old: Team, val new: Team) : HexagonDataEvent(data) {

  init {
    require(old !== new) { "There is no team change if the old and the new team are the same!" }
  }

  companion object {
    internal val listeners = GdxUtilsArray<(TeamChangeHexagonDataEvent) -> Unit>()
  }
}

/**
 * Simplify event listening by automatically removing the event by calling [dispose] on this object
 *
 * Call [create] for the best experience
 *
 */
class SimpleEventListener<T : HexagonDataEvent>(clazz: KClass<T>, private val run: (T) -> Unit) : Disposable {

  private val eventList = (getEventList(clazz) as GdxUtilsArray<(T) -> Unit>)

  init {
    require(!clazz.isAbstract) { "Cannot create a listener for an abstract event: $clazz" }
    eventList.add(run)
  }

  override fun dispose() {
    eventList.removeValue(run, true)
  }

  companion object {
    inline fun <reified T : HexagonDataEvent> create(noinline run: (T) -> Unit) = SimpleEventListener(T::class, run)
  }
}
