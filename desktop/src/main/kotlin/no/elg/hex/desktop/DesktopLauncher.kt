package no.elg.hex.desktop

import com.badlogic.gdx.Files.FileType.Internal
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.xenomachina.argparser.ArgParser
import no.elg.hex.ApplicationArgumentsParser
import no.elg.hex.Hex
import no.elg.hex.util.defaultDisplayMode

fun main(args: Array<String>) {
  Hex.args = ArgParser(args).parseInto(::ApplicationArgumentsParser)

  val config = LwjglApplicationConfiguration()

  config.width = defaultDisplayMode.width / 2
  config.height = defaultDisplayMode.height / 2

  config.backgroundFPS = 10
  config.foregroundFPS = 9999
  config.vSyncEnabled = false // Why not? it's not like this is a competitive FPS
  config.samples = 16 // max out the samples as this isn't a very heavy game.
  config.useHDPI = true
  config.addIcon("icons/icon32.png", Internal)
  config.addIcon("icons/icon128.png", Internal)

  LwjglApplicationConfiguration.disableAudio = true

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
