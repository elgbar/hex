package no.elg.hex.platform.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.ReadableLwjgl3ApplicationConfiguration
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.platform.Platform
import no.elg.hex.platform.PlatformType
import no.elg.hex.util.compressB85IfEnabled
import no.elg.hex.util.loadProperties
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

class DesktopPlatform(val config: ReadableLwjgl3ApplicationConfiguration) : Platform {

  override val version: String? by lazy { loadProperties("version.properties").getProperty("version", null) }

  override val canLimitFps: Boolean = true

  override val canToggleVsync: Boolean = true

  override val canSetMSAA: Boolean = true
  override val defaultMSAA: Int = 16

  override val canControlAudio: Boolean = true

  override val type: PlatformType = PlatformType.DESKTOP

  override val vsync: Boolean get() = config.isVSync

  override fun trace(tag: String, exception: Throwable?, message: String) {
    Gdx.app.debug("TRACE | $tag", message + (exception?.let { "\n${it.stackTraceToString()}" } ?: ""))
  }

  override fun writeToClipboard(label: String, data: Any): Boolean {
    try {
      val writeValueAsString = compressB85IfEnabled(Hex.mapper.writeValueAsString(data))
      Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(writeValueAsString), null)
      return true
    } catch (e: HeadlessException) {
      Gdx.app.error("DesktopPlatform", "No system clipboard exists", e)
    } catch (e: Exception) {
      Gdx.app.error("DesktopPlatform", "Failed to write to clipboard", e)
    }
    MessagesRenderer.publishError("Failed to copy to clipboard")
    return false
  }

  override fun readStringFromClipboard(): String? =
    try {
      val systemClipboard = Toolkit.getDefaultToolkit().systemClipboard
      (systemClipboard.getData(DataFlavor.stringFlavor) as? String)
    } catch (e: Exception) {
      Gdx.app.error("DesktopPlatform", "Failed to read string from clipboard", e)
      null
    }
}