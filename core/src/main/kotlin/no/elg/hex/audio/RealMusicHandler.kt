package no.elg.hex.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import ktx.assets.disposeSafely
import no.elg.hex.Hex
import no.elg.hex.Settings

class RealMusicHandler : MusicHandler {
  private var playingMusic: Music? = null
    set(value) {
      if (field == value) {
        return
      }
      field?.stop()
      field = value
      value?.apply {
        updateMusicVolume()
        if (Settings.musicPaused) {
          pause()
        } else {
          play()
        }
      }
    }

  override val icon: TextureAtlas.AtlasRegion get() = if (Settings.musicPaused) Hex.assets.muted else Hex.assets.unmuted
  override val iconSelected: TextureAtlas.AtlasRegion get() = if (Settings.musicPaused) Hex.assets.mutedSelected else Hex.assets.unmutedSelected

  override fun updateMusicVolume() {
    playingMusic?.volume = Settings.masterVolume * Settings.musicVolume
  }

  /**
   * Play a random song and change when each of them ends
   */
  override fun playRandom() {
    if (Hex.assets.songs.size <= 1) {
      // Only one song, play it on a loop
      loop(Hex.assets.songs.firstOrNull())
    } else {
      playingMusic = Hex.assets.songs.randomOrNull()?.apply {
        isLooping = false
        setOnCompletionListener {
          playingMusic = Hex.assets.songs.filter { it != this }.randomOrNull()
        }
      }
    }
  }

  /**
   * Play the given music on a loop
   */
  override fun loop(newMusic: Music?) {
    playingMusic = newMusic?.apply {
      isLooping = true
      setOnCompletionListener(null)
    }
  }

  /**
   * @return If the music was toggled
   */
  override fun toggleMute(): Boolean {
    playingMusic?.run {
      if (Settings.musicPaused) {
        pause()
        return true
      } else {
        play()
        return true
      }
    }
    return false
  }

  override fun dispose() {
    playingMusic.disposeSafely()
  }
}