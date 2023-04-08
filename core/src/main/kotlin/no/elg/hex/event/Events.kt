package no.elg.hex.event

import com.badlogic.gdx.utils.Array
import kotlinx.coroutines.runBlocking
import ktx.async.onRenderingThread
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmName

/**
 * @author Elg
 */
object Events {

  private const val EXPECTED_LISTENERS_FIELD_NAME = "listeners"

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

  fun <T : Event> fireEvent(event: T) {
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
  fun getEventList(eventClass: KClass<out Event>): Array<*> {
    val companionClass = eventClass.companionObject ?: error("No companion object found for ${eventClass.simpleName}")
    val companionInstance = eventClass.companionObjectInstance ?: error("No companion object found for ${eventClass.simpleName}")
    val listenersField = companionClass.declaredMembers.firstOrNull { it.name == EXPECTED_LISTENERS_FIELD_NAME }
      ?: error("Failed to find a member within ${eventClass.simpleName}s companion object called $EXPECTED_LISTENERS_FIELD_NAME")

    // Check that the return type is GdxUtilsArray (though we still do not know the generic parameter!)
    require(listenersField.returnType.isSubtypeOf(Array::class.createType(arguments = listOf(KTypeProjection.STAR)))) {
      "The '$EXPECTED_LISTENERS_FIELD_NAME' member must be an instance of ${Array::class.jvmName}"
    }

    // The first (and hopefully only) parameter is the instance parameter
    require(listenersField.parameters.size == 1) {
      "The '$EXPECTED_LISTENERS_FIELD_NAME' member cannot have any arguments. Found the following arguments ${listenersField.parameters}"
    }

    return listenersField.call(companionInstance) as Array<*>
  }
}