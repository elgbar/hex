package no.elg.hex.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import no.elg.hex.Hex

object DisabledMusicHandler : MusicHandler {
  override val icon: TextureAtlas.AtlasRegion get() = Hex.assets.muted
  override val iconSelected: TextureAtlas.AtlasRegion get() = Hex.assets.muted

  override fun updateMusicVolume() = Unit

  override fun playRandom() = Unit

  override fun loop(newMusic: Music?) = Unit

  override fun toggleMute(): Boolean = false

  override fun dispose() = Unit
}