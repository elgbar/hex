package no.elg.hex.platform.desktop

import no.elg.hex.platform.Platform
import no.elg.hex.util.loadProperties

class DesktopPlatform : Platform {

  override val version: String by lazy { loadProperties("version.properties").getProperty("version") }

}