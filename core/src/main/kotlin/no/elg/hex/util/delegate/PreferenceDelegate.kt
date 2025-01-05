package no.elg.hex.util.delegate

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import no.elg.hex.event.Events
import no.elg.hex.event.SimpleEventListener
import no.elg.hex.event.events.SettingsChangeEvent
import no.elg.hex.util.debug
import no.elg.hex.util.toEnumOrNull
import no.elg.hex.util.trace
import kotlin.reflect.KProperty

open class PreferenceDelegate<T : Any>(
  /**
   * Initial value this setting should have
   */
  internal val initialValue: T,
  /**
   * What preference this setting should be saved to
   *
   * @see no.elg.hex.Hex.launchPreference
   */
  internal val preferences: Preferences = Companion.preferences,
  /**
   * If changing this requires a restart to apply
   */
  internal val requireRestart: Boolean = false,
  /**
   * How far up this setting should be.
   * A lower number means the setting will be placed higher in the settings screen.
   * If multiple settings have the same [priority] they are listed alphabetically
   */
  val priority: Int = 1000,
  /**
   * If [afterChange] should be called when the program starts
   */
  runAfterChangeOnInit: Boolean = true,

  /**
   * Method to call *after* a change is applied
   */
  val afterChange: ((delegate: PreferenceDelegate<T>, old: T, new: T) -> Unit)? = null,
  /**
   * If this setting should be hidden in the settings screen
   */
  private val shouldHide: (T) -> Boolean = { false },
  // Impl note: 'invalidate' must be the last parameter
  /**
   * A function to test if a given value is **in**valid
   */
  val invalidate: (T) -> Boolean = { false }
) {

  private var changed = false
  private var currentValue: T? = null
  private lateinit var initialLoadedValue: T

  @Suppress("unused")
  private val eventListener = SimpleEventListener.create<SettingsChangeEvent<T>> { (delagate, old, new) ->
    if (delagate === this) {
      afterChange?.invoke(this, old, new)
    }
  }

  fun displayRestartWarning() = requireRestart && changed

  init {
    require(initialValue is Number || initialValue is String || initialValue is Boolean || initialValue is Char || initialValue is Enum<*>) {
      "Type must either be Enum, Number, String, Char, or Boolean. The given type us ${initialValue::class.simpleName}"
    }
    require(!invalidate(initialValue)) { "The initial value cannot be invalid" }

    if (runAfterChangeOnInit) {
      Gdx.app.postRunnable {
        Events.fireEvent(SettingsChangeEvent(this, initialValue, currentValue ?: initialValue))
      }
    }
  }

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    currentValue?.also { return it }

    val propertyName = property.name
    if (preferences.contains(propertyName)) {
      val value = readPreference(propertyName)

      if (value != null && !invalidate(value)) {
        if (!::initialLoadedValue.isInitialized) {
          initialLoadedValue = value
        }
        currentValue = value
        return value
      }

      Gdx.app.log(
        "PREF",
        "Invalid preference value ($value) found for '$propertyName', restoring initial value ($initialValue)"
      )
      setValue(thisRef, property, initialValue)
    }
    if (!::initialLoadedValue.isInitialized) {
      initialLoadedValue = initialValue
    }
    return initialValue
  }

  private fun readPreference(propertyName: String): T? {
    if (!preferences.contains(propertyName)) {
      return null
    }

    @Suppress("UNCHECKED_CAST")
    return when (initialValue) {
      is Boolean -> preferences.getBoolean(propertyName, initialValue as Boolean)
      is Int -> preferences.getInteger(propertyName, initialValue as Int)
      is Float -> preferences.getFloat(propertyName, initialValue as Float)
      is CharSequence -> preferences.getString(propertyName, initialValue.toString())
      is Long -> preferences.getLong(propertyName, initialValue as Long)

      is Byte -> preferences.getInteger(propertyName, (initialValue as Byte).toInt()).toByte()
      is Short -> preferences.getInteger(propertyName, (initialValue as Short).toInt()).toShort()
      is Char -> preferences.getInteger(propertyName, (initialValue as Char).code).toChar()
      is Double -> preferences.getFloat(propertyName, (initialValue as Double).toFloat()).toDouble()
      is Enum<*> -> preferences.getString(propertyName, null)?.toEnumOrNull(initialValue::class) ?: initialValue

      else -> error("Nullable types are not allowed")
    } as T
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    val propertyName = property.name
    if (invalidate(value)) {
      Gdx.app.trace("SETTINGS") { "Will not set $propertyName to $value as it is invalid" }
      return
    }

    val old = currentValue ?: initialValue
    currentValue = value

    if (value != old) {
      if (!changed) {
        changed = true
      } else if (initialLoadedValue == value) {
        changed = false
      }
    }

    Gdx.app.debug("SETTINGS") { "Changing '$propertyName' from '$old' to '$value'" }

    when (initialValue) {
      is Boolean -> preferences.putBoolean(propertyName, value as Boolean)
      is Int -> preferences.putInteger(propertyName, value as Int)
      is Float -> preferences.putFloat(propertyName, value as Float)
      is CharSequence -> preferences.putString(propertyName, value.toString())
      is Long -> preferences.putLong(propertyName, value as Long)
      is Byte -> preferences.putInteger(propertyName, (value as Byte).toInt())
      is Char -> preferences.putInteger(propertyName, (value as Char).code)
      is Short -> preferences.putInteger(propertyName, (value as Short).toInt())
      is Double -> preferences.putFloat(propertyName, (value as Double).toFloat())
      is Enum<*> -> preferences.putString(propertyName, (value as Enum<*>).name)
      else -> error("Preferences of type ${initialValue::class.simpleName} is not allowed")
    }
    preferences.flush()
    Events.fireEvent(SettingsChangeEvent(this, old, value))
  }

  fun shouldHide(): Boolean = shouldHide(currentValue ?: initialValue)

  companion object {
    private val preferences: Preferences by lazy {
      val name = this::class.qualifiedName

      Gdx.app.trace("PREFS") { "Using preference name $name" }
      Gdx.app.getPreferences(name)
    }
  }
}