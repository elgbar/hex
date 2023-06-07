package no.elg.hex.util.delegate

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import no.elg.hex.util.debug
import no.elg.hex.util.toEnumOrNull
import no.elg.hex.util.trace
import kotlin.reflect.KClass
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
   * How far up this setting should be, lower means higher in the settings screen. If multiple settings have the same [priority] they are listed alphabetically
   */
  val priority: Int = 1000,
  /**
   * If [onChange] should be called when the program starts
   */
  runOnChangeOnInit: Boolean = true,

  /**
   * only apply settings when leaving the settings screen
   */
  val applyOnChangeOnSettingsHide: Boolean = false,
  /**
   * Method to call when a change is applied. The first argument will always be `this`. If the onChange
   */
  val onChange: ((delegate: PreferenceDelegate<T>, old: T, new: T) -> T)? = null,
  private val shouldHide: (T) -> Boolean = { false },
  /**
   * A function to test if a given value is **in**valid
   */
  val invalidate: (T) -> Boolean = { false } // Note: this must be the last parameter
) {

  private var changed = false
  private var currentValue: T? = null
  private lateinit var initialLoadedValue: T

  fun displayRestartWarning() = requireRestart && changed

  init {
    @Suppress("LeakingThis")
    require(initialValue is Number || initialValue is String || initialValue is Boolean || initialValue is Char || initialValue is Enum<*>) {
      "Type must either be Enum, Number, String, Char, or Boolean. The given type us ${initialValue::class.simpleName}"
    }
    require(!invalidate(initialValue)) { "The initial value cannot be invalid" }

    if (runOnChangeOnInit) {
      Gdx.app.postRunnable {
        onChange?.invoke(this, initialValue, currentValue ?: initialValue)
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
      is Enum<*> -> preferences.getString(propertyName, initialValue.toString())
        .toEnumOrNull(initialValue::class as KClass<out Enum<*>>)

      else -> error("Nullable types are not allowed")
    } as T
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    val propertyName = property.name
    if (invalidate(value)) {
      Gdx.app.trace("PREF") { "Will not set $propertyName to $value as it is invalid" }
      return
    }

    val old = currentValue ?: initialValue
    val newValue = if (onChange != null && !applyOnChangeOnSettingsHide) {
      Gdx.app.trace("PREF") { "Calling on change for setting $propertyName" }
      onChange.invoke(this, old, value)
    } else {
      value
    }

    if (invalidate(newValue)) {
      Gdx.app.trace("PREF") { "Will not set $propertyName to $value as it is invalid" }
      return
    }

    currentValue = newValue
    if (currentValue != old) {
      if (!changed) {
        changed = true
      } else if (initialLoadedValue == currentValue) {
        changed = false
      }
    }

    Gdx.app.debug("SETTINGS") { "Changing '$propertyName' from '$old' to '$currentValue'" }

    when (initialValue) {
      is Boolean -> preferences.putBoolean(propertyName, currentValue as Boolean)
      is Int -> preferences.putInteger(propertyName, currentValue as Int)
      is Float -> preferences.putFloat(propertyName, currentValue as Float)
      is CharSequence -> preferences.putString(propertyName, currentValue.toString())
      is Long -> preferences.putLong(propertyName, currentValue as Long)
      is Byte -> preferences.putInteger(propertyName, (currentValue as Byte).toInt())
      is Char -> preferences.putInteger(propertyName, (currentValue as Char).code)
      is Short -> preferences.putInteger(propertyName, (currentValue as Short).toInt())
      is Double -> preferences.putFloat(propertyName, (currentValue as Double).toFloat())
      is Enum<*> -> preferences.putString(propertyName, (currentValue as Enum<*>).name)
      else -> error("Preferences of type ${initialValue::class.simpleName} is not allowed")
    }
    preferences.flush()
  }

  fun shouldHide(): Boolean = shouldHide(currentValue ?: initialValue)

  fun hide(property: KProperty<*>) {
    if (applyOnChangeOnSettingsHide && changed) {
      val old = currentValue ?: initialValue
      onChange?.invoke(this, old, getValue(null, property))
    }
  }

  companion object {
    private val preferences: Preferences by lazy {
      val name = this::class.qualifiedName

      Gdx.app.trace("PREFS") { "Using preference name $name" }
      Gdx.app.getPreferences(name)
    }
  }
}