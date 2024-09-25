package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.ALT_LEFT
import com.badlogic.gdx.Input.Keys.CONTROL_LEFT
import com.badlogic.gdx.Input.Keys.CONTROL_RIGHT
import com.badlogic.gdx.Input.Keys.ENTER
import com.badlogic.gdx.Input.Keys.M
import no.elg.hex.Hex
import no.elg.hex.Settings

object GlobalInputProcessor : AbstractInput() {

  private var oldWidth = 0
  private var oldHeight = 0

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      CONTROL_LEFT, CONTROL_RIGHT, M -> {
        if (Gdx.input.isKeyPressed(M) && (Gdx.input.isKeyPressed(CONTROL_LEFT) || Gdx.input.isKeyPressed(CONTROL_RIGHT))) {
          Settings.musicPaused = !Settings.musicPaused
          return true
        }
      }
      ALT_LEFT, ENTER -> {
        if (Gdx.input.isKeyPressed(ALT_LEFT) && Gdx.input.isKeyPressed(ENTER)) {
          val mode = Gdx.graphics.displayMode
          if (Gdx.graphics.isFullscreen) {
            if (Hex.scale > 1) {
              Gdx.graphics.setWindowedMode(1920, 1080)
            } else {
              Gdx.graphics.setWindowedMode(1280, 720)
            }
          } else {
            oldWidth = mode.width
            oldHeight = mode.height
            Gdx.graphics.setFullscreenMode(mode)
          }
          return true
        }
      }
    }
    return false
  }
}