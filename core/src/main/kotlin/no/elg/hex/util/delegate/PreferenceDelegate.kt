package no.elg.hex.util.delegate

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import no.elg.hex.util.trace
import kotlin.reflect.KProperty

class PreferenceDelegate<T : Any>(private val initialValue: T) {

  init {
    require(initialValue is Number || initialValue is String || initialValue is Boolean || initialValue is Char) {
      "Type must either be a Number, String, Char, or a Boolean. The given type us ${initialValue::class.simpleName}"
    }
  }

  @Suppress("UNCHECKED_CAST")
  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    val propertyName = property.name
    if (preferences.contains(propertyName)) {
      return when (initialValue) {
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
    }
    return initialValue
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    val propertyName = property.name
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
      else -> error("The type ${initialValue::class.simpleName} types are not allowed")
    }
    preferences.flush()
  }


  companion object {
    private val preferences: Preferences by lazy {
      val name = this::class.qualifiedName
      Gdx.app.trace("PREF DEL", "Using preference name $name")
      Gdx.app.getPreferences(name)
    }
  }

}
