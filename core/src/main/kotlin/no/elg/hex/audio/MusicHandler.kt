package no.elg.hex.audio

import com.badlogic.gdx.audio.Music
import no.elg.hex.Hex
import no.elg.hex.Hex.audioDisabled
import no.elg.hex.Settings

class MusicHandler {

  private var music: Music? = null
    set(value) {
      if (audioDisabled || field == value) {
        return
      }
      field?.stop()
      field = value
      value?.apply {
        volume = Settings.masterVolume * Settings.musicVolume
        play()
      }
    }

  fun updateMusicVolume() {
    music?.volume = Settings.masterVolume * Settings.musicVolume
  }

  /**
   * Play a random song and change when each of them ends
   */
  fun playRandom() {
    if (audioDisabled) {
      return
    }
    music?.stop()
    music = Hex.assets.songs.randomOrNull()?.apply {
      isLooping = false
      setOnCompletionListener {
        music = Hex.assets.songs.filter { it != this }.randomOrNull()
      }
    }
  }

  /**
   * Play the given music on a loop
   */
  fun loop(newMusic: Music?)  {
    if (audioDisabled) {
      return
    }
    music?.stop()
    music = newMusic?.apply {
      isLooping = true
      setOnCompletionListener(null)
    }
  }
}