package no.elg.hex.util.delegate

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
internal class PreferenceDelegateTest {

  @BeforeAll
  fun beforeAll() {
    HeadlessApplication(object : ApplicationAdapter() {})
  }

  @AfterAll
  fun afterAll() {
    Gdx.app.exit()
  }

  private object TestSettings {

    var boolPref by PreferenceDelegate<Boolean>(true)
    var floatPref by PreferenceDelegate<Float>(1f)
    var doublePref by PreferenceDelegate<Double>(0.0)
    var bytePref by PreferenceDelegate<Byte>(1)
    var shortPref by PreferenceDelegate<Short>(1)
    var intPref by PreferenceDelegate<Int>(1)
    var longPref by PreferenceDelegate<Long>(1L)
    var stringPref by PreferenceDelegate<String>("yteye")
    var charPref by PreferenceDelegate<Char>('y')
  }

  @Test
  fun test() {
    println(TestSettings.bytePref)
    println(TestSettings.doublePref)
    println(TestSettings.floatPref)
    println(TestSettings.shortPref)
    println(TestSettings.intPref)
    println(TestSettings.longPref)
    println(TestSettings.boolPref)
    println(TestSettings.stringPref)
    println(TestSettings.charPref)

    TestSettings.bytePref = 3
    TestSettings.doublePref = -14.0
    TestSettings.floatPref = 99f
    TestSettings.shortPref = 9
    TestSettings.intPref = 8
    TestSettings.longPref = 7
    TestSettings.boolPref = false
    TestSettings.stringPref = "string"
    TestSettings.charPref = 'o'

    println(TestSettings.bytePref)
    println(TestSettings.doublePref)
    println(TestSettings.floatPref)
    println(TestSettings.shortPref)
    println(TestSettings.intPref)
    println(TestSettings.longPref)
    println(TestSettings.boolPref)
    println(TestSettings.stringPref)
    println(TestSettings.charPref)
  }
}