package no.elg.hex.platform.desktop

import com.badlogic.gdx.Gdx
import no.elg.hex.platform.Platform
import no.elg.hex.platform.PlatformType
import no.elg.hex.util.loadProperties

class DesktopPlatform : Platform {

  override val version: String? by lazy { loadProperties("version.properties").getProperty("version", null) }

  override val canLimitFps: Boolean = true

  override val canToggleVsync: Boolean = true

  override val canSetMSAA: Boolean = true
  override val defaultMSAA: Int = 16

  override val canControlAudio: Boolean = true

  override val type: PlatformType = PlatformType.DESKTOP

  override fun trace(tag: String, exception: Throwable?, message: String) {
    Gdx.app.debug("TRACE | $tag", message + (exception?.let { "\n${it.stackTraceToString()}" } ?: ""))
  }
}