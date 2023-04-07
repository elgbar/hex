package no.elg.hex.platform.desktop

import com.badlogic.gdx.Files.FileType.Internal
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Preferences
import com.badlogic.gdx.backends.lwjgl3.ReadableLwjgl3ApplicationConfiguration
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import no.elg.hex.ApplicationArgumentsParser
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.Settings.MSAA_SAMPLES_PATH
import no.elg.hex.util.defaultDisplayMode

fun main(args: Array<String>) {
  val config = ReadableLwjgl3ApplicationConfiguration()

  Hex.args = mainBody { ArgParser(args).parseInto(::ApplicationArgumentsParser) }
  Hex.launchPreference = Lwjgl3Preferences(Hex.LAUNCH_PREF, config.preferencesDirectory)

  Hex.platform = DesktopPlatform()
  Hex.audioDisabled = !Hex.launchPreference.getBoolean(Settings.ENABLE_AUDIO_PATH)

  config.setWindowedMode(defaultDisplayMode.width / 2, defaultDisplayMode.height / 2)
//  config.initialBackgroundColor = Hex.backgroundColor

//  config.backgroundFPS = 10
  config.foregroundFPS = 0
  config.isVSync = Hex.launchPreference.getBoolean(Settings.VSYNC_PATH, true)
  config.setWindowIcon(Internal, "icons/icon32.png", "icons/icon128.png")

  val samples = if (Hex.launchPreference.contains(MSAA_SAMPLES_PATH)) {
    Hex.launchPreference.getInteger(MSAA_SAMPLES_PATH)
  } else {
    16 // default value
  }
  val c = Hex.backgroundColor
  config.setBackBufferConfig((c.r * 255).toInt(), (c.g * 255).toInt(), (c.b * 255).toInt(), 1, config.depth, config.stencil, samples)
  config.title = "Hex"

  Lwjgl3Application(Hex, config)
}