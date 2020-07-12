package no.elg.hex.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import no.elg.hex.Hex
import src.no.elg.hex.InputHandler

fun main() {
  val config = LwjglApplicationConfiguration()

  if (InputHandler.scale > 1) {
    config.width = 1920
    config.height = 1080
  } else {
    config.width = 1280
    config.height = 720
  }

  config.backgroundFPS = 10
  config.foregroundFPS = 9999
  config.vSyncEnabled = false //Why not? it's not like this is a competitive FPS
  config.samples = 16 //max out the samples as this isn't a very heavy game.
  config.useHDPI = true;
  LwjglApplication(Hex, config)
}
