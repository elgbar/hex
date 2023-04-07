package no.elg.hex.platform.desktop

import com.badlogic.gdx.Gdx
import no.elg.hex.platform.Platform
import no.elg.hex.util.loadProperties

class DesktopPlatform : Platform {

  override val version: String by lazy { loadProperties("version.properties").getProperty("version") }

  override val canLimitFps: Boolean = true

  override val canToggleVsync: Boolean = true

  override fun setFps(fps: Int) {
    Gdx.graphics.setForegroundFPS(fps)
  }
}