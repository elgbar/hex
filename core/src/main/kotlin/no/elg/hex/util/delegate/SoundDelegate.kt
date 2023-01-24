package no.elg.hex.util.delegate

import com.badlogic.gdx.audio.Sound
import no.elg.hex.Assets
import no.elg.hex.Hex
import kotlin.reflect.KProperty

/**
 * @author Elg
 */
class SoundDelegate(private val filePath: String) {

  private lateinit var sound: Sound

  operator fun getValue(thisRef: Any?, property: KProperty<*>): Sound? {
    return if (Hex.assets.audioLoaded(false)) {
      if (!::sound.isInitialized) {
        sound = Hex.assets.get(filePath, Assets.SOUND)
      }
      sound
    } else {
      null
    }
  }
}
