package no.elg.hex.util.delegate

import com.badlogic.gdx.audio.Sound
import no.elg.hex.Hex
import no.elg.hex.util.fetch
import kotlin.reflect.KProperty

/**
 * @author Elg
 */
class SoundAlternativeDelegate(private val filePath: String, private val alternatives: IntRange) {

  init {
    require(filePath.contains("%d")) { "File path must contain a single '%d'" }
  }

  private lateinit var alternativeSounds: MutableList<Sound>

  operator fun getValue(thisRef: Any?, property: KProperty<*>): Sound? =
    if (Hex.assets.audioLoaded(false)) {
      if (!::alternativeSounds.isInitialized) {
        alternativeSounds = mutableListOf()
        for (i in alternatives) {
          alternativeSounds.add(Hex.assets.fetch(filePath.format(i)))
        }
        require(alternativeSounds.isNotEmpty()) { "Failed to load any sounds of $filePath. Expected ${alternatives.count()} alternatives" }
      }
      alternativeSounds.random()
    } else {
      null
    }
}