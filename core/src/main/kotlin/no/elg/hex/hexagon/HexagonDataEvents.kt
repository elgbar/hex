package no.elg.hex.hexagon

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

  fun onTeamChange(listener: (TeamChangeHexagonDataEvent) -> Unit) {
    TeamChangeHexagonDataEvent.listeners.add(listener)
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

    for (listener in listeners) {
      requireNotNull(listener) { "Null function given as a listener!" }
      try {
        (listener as Function1<T, Unit>)(event)
      } catch (e: ClassCastException) {
        error("Registered function is not of correct type. Expected a function (${event::class.simpleName} -> Unit) but found a ${listener::class.simpleName}")
      }
    }
  }

  private fun getEventList(eventClass: KClass<out HexagonDataEvent>): GdxUtilsArray<*> {
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
 * Called when the team of a [HexagonData] will be changed. Event is called before the change occur. (In the future, if needed, this event can be made cancellable)
 */
class TeamChangeHexagonDataEvent(data: HexagonData, val old: Team, val new: Team) : HexagonDataEvent(data) {

  init {
    require(old !== new) { "There is no team change if the old and the new team are the same!" }
  }

  companion object {
    internal val listeners = GdxUtilsArray<(TeamChangeHexagonDataEvent) -> Unit>()
  }
}
