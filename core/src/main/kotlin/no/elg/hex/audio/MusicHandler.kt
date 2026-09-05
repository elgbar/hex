package no.elg.hex.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Disposable
import ktx.assets.disposeSafely
import no.elg.hex.Settings
import no.elg.hex.util.isLazyInitialized

sealed interface MusicHandler : Disposable {
  val icon: TextureAtlas.AtlasRegion
  val iconSelected: TextureAtlas.AtlasRegion
  fun updateMusicVolume()

  /**
   * Play a random song and change when each of them ends
   */
  fun playRandom()

  /**
   * Play the given music on a loop
   */
  fun loop(newMusic: Music?)

  /**
   * @return If the music was toggled
   */
  fun toggleMute(): Boolean

  companion object {

    var instance: MusicHandler? = null
      private set(value) {
        field?.disposeSafely()
        field = value
      }

    /**
     * If audio is enabled and we are playing something
     */
    val audioPlaying get() = audioEnabled && !Settings.musicPaused

    /**
     * If audio is disabled or we are not playing something
     */
    val audioNotPlaying get() = !audioPlaying

    val audioEnabled
      get() = when (instance) {
        is RealMusicHandler -> true
        DisabledMusicHandler -> false
        null -> error("Audio not setup yet!")
      }
    val audioDisabled get() = !audioEnabled

    val isAudioSetup get() = instance != null

    fun disableAudio() {
      instance = DisabledMusicHandler
    }

    fun setupAudio(enableAudio: Boolean) {
      require(instance == null) { "Cannot setup auto twice" }
      instance = if (enableAudio) {
        RealMusicHandler()
      } else {
        DisabledMusicHandler
      }
    }
  }
}