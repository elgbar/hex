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
        if (Settings.musicPaused) {
          pause()
        } else {
          play()
        }
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
  fun loop(newMusic: Music?) {
    if (audioDisabled) {
      return
    }
    music = newMusic?.apply {
      isLooping = true
      setOnCompletionListener(null)
    }
  }

  /**
   * @return If the music was toggled
   */
  fun toggleMute(): Boolean {
    if (audioDisabled) {
      return false
    }
    music?.run {
      if (isPlaying) {
        pause()
        return true
      } else if (!isPlaying) {
        play()
        return true
      }
      return false
    }
    return false
  }
}