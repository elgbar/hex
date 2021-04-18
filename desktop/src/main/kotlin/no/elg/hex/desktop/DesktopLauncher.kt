package no.elg.hex.desktop

import com.badlogic.gdx.Files.FileType.Internal
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import no.elg.hex.ApplicationArgumentsParser
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.Settings.MSAA_SAMPLES_PATH
import no.elg.hex.util.defaultDisplayMode

fun main(args: Array<String>) {

  val config = LwjglApplicationConfiguration()

  Hex.args = mainBody { ArgParser(args).parseInto(::ApplicationArgumentsParser) }
  Hex.launchPreference = LwjglPreferences(Hex.LAUNCH_PREF, config.preferencesDirectory)

  LwjglApplicationConfiguration.disableAudio = Hex.launchPreference.getBoolean(Settings.DISABLE_AUDIO_PATH)

  config.width = defaultDisplayMode.width / 2
  config.height = defaultDisplayMode.height / 2

  config.initialBackgroundColor = Hex.backgroundColor

  config.backgroundFPS = 10
  config.foregroundFPS = 9999
  config.vSyncEnabled = true // Why not? it's not like this is a competitive FPS
  config.useHDPI = true
  config.addIcon("icons/icon32.png", Internal)
  config.addIcon("icons/icon128.png", Internal)

  if (Hex.launchPreference.contains(MSAA_SAMPLES_PATH)) {
    config.samples = Hex.launchPreference.getInteger(MSAA_SAMPLES_PATH)
  } else {
    config.samples = 16 // default value
  }

  config.title = "Hex"
  if (Hex.args.mapEditor) {
    config.title += " - Map Editor"
  }
  if (Hex.args.trace) {
    config.title += " (trace)"
  } else if (Hex.args.debug) {
    config.title += " (debug)"
  }

  LwjglApplication(Hex, config)
}
