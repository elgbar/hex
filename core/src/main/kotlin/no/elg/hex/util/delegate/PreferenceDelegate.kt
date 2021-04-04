package no.elg.hex.util.delegate

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import no.elg.hex.util.trace
import kotlin.reflect.KProperty

class PreferenceDelegate<T : Any>(
  private val initialValue: T,
  private val preferences: Preferences = Companion.preferences,
  private val requireRestart: Boolean = false,
  val onChange: ((old: T, new: T) -> Unit)? = null,
  val invalidate: (T) -> Boolean = { false }
) {

  private var changed = false
  private var currentValue: T? = null

  fun displayRestartWarning() = requireRestart && changed

  init {
    require(initialValue is Number || initialValue is String || initialValue is Boolean || initialValue is Char) {
      "Type must either be a Number, String, Char, or a Boolean. The given type us ${initialValue::class.simpleName}"
    }
    require(!invalidate(initialValue)) { "The initial value cannot be invalid" }

    Gdx.app.postRunnable {
      onChange?.invoke(initialValue, currentValue ?: initialValue)
    }
  }

  @Suppress("UNCHECKED_CAST")
  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    currentValue?.also { return it }

    val propertyName = property.name
    if (preferences.contains(propertyName)) {
      val value = when (initialValue) {
        is Boolean -> preferences.getBoolean(propertyName, initialValue as Boolean)
        is Int -> preferences.getInteger(propertyName, initialValue as Int)
        is Float -> preferences.getFloat(propertyName, initialValue as Float)
        is CharSequence -> preferences.getString(propertyName, initialValue.toString())
        is Long -> preferences.getLong(propertyName, initialValue as Long)

        is Byte -> preferences.getInteger(propertyName, (initialValue as Byte).toInt()).toByte()
        is Short -> preferences.getInteger(propertyName, (initialValue as Short).toInt()).toShort()
        is Char -> preferences.getInteger(propertyName, (initialValue as Char).toInt()).toChar()
        is Double -> preferences.getFloat(propertyName, (initialValue as Double).toFloat()).toDouble()
        else -> error("Nullable types are not allowed")
      } as T

      if (!invalidate(value)) {
        currentValue = value
        return value
      }

      Gdx.app.log("PREF", "Invalid preference value ($value) found for '$propertyName', restoring initial value ($initialValue)")
      setValue(thisRef, property, initialValue)
    }
    return initialValue
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    val propertyName = property.name
    if (invalidate(value)) {
      Gdx.app.trace("PREF", "Will not set $propertyName to $value as it is invalid")
      return
    }

    when (initialValue) {
      is Boolean -> preferences.putBoolean(propertyName, value as Boolean)
      is Int -> preferences.putInteger(propertyName, value as Int)
      is Float -> preferences.putFloat(propertyName, value as Float)
      is CharSequence -> preferences.putString(propertyName, value.toString())
      is Long -> preferences.putLong(propertyName, value as Long)
      is Byte -> preferences.putInteger(propertyName, (value as Byte).toInt())
      is Char -> preferences.putInteger(propertyName, (value as Char).toInt())
      is Short -> preferences.putInteger(propertyName, (value as Short).toInt())
      is Double -> preferences.putFloat(propertyName, (value as Double).toFloat())
      else -> error("Preferences of type ${initialValue::class.simpleName} is not allowed")
    }

    val old = currentValue ?: initialValue
    currentValue = value
    preferences.flush()
    changed = true
    if (onChange != null) {
      Gdx.app.trace("PREF", "Calling on change for setting $propertyName")
      onChange.invoke(old, value)
    }
  }

  companion object {
    private val preferences: Preferences by lazy {
      val name = this::class.qualifiedName

      Gdx.app.trace("PREFS", "Using preference name $name")
      Gdx.app.getPreferences(name)
    }
  }
}
