package no.elg.hex.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.g2d.TextureAtlas
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

  val icon: TextureAtlas.AtlasRegion get() = if (Settings.musicPaused) Hex.assets.muted else Hex.assets.unmuted
  val iconSelected: TextureAtlas.AtlasRegion get() = if (Settings.musicPaused) Hex.assets.mutedSelected else Hex.assets.unmutedSelected

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
    if (Hex.assets.songs.size <= 1) {
      // Only one song, play it on a loop
      loop(Hex.assets.songs.firstOrNull())
    } else {
      music = Hex.assets.songs.randomOrNull()?.apply {
        isLooping = false
        setOnCompletionListener {
          music = Hex.assets.songs.filter { it != this }.randomOrNull()
        }
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
    music?.run {
      if (Settings.musicPaused || audioDisabled) {
        pause()
        return true
      } else {
        play()
        return true
      }
    }
    return false
  }
}