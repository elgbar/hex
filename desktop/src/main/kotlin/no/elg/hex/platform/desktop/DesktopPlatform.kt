package no.elg.hex.platform.desktop

import no.elg.hex.platform.Platform
import no.elg.hex.platform.PlatformType
import no.elg.hex.util.loadProperties

class DesktopPlatform : Platform {

  override val version: String by lazy { loadProperties("version.properties").getProperty("version") }

  override val canLimitFps: Boolean = true

  override val canToggleVsync: Boolean = true

  override val canSetMSAA: Boolean = true

  override val canControlAudio: Boolean = true

  override val type: PlatformType = PlatformType.DESKTOP
}