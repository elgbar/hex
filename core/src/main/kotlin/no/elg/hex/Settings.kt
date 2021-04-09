package no.elg.hex

import com.badlogic.gdx.Gdx
import no.elg.hex.hud.GLProfilerRenderer
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.util.delegate.PreferenceDelegate
import no.elg.hex.util.resetAllIslandProgress

@Suppress("unused")
object Settings {

  var confirmEndTurn by PreferenceDelegate(true)
  var confirmSurrender by PreferenceDelegate(true)

  var showFps by PreferenceDelegate(false)
  var limitFps by PreferenceDelegate(false, onChange = { _, _, new -> updateForegroundFPS(); return@PreferenceDelegate new })
  var targetFps by PreferenceDelegate(30, onChange = { _, _, new -> updateForegroundFPS(); return@PreferenceDelegate new }) { it < 5 }

  const val MSAA_SAMPLES_PATH = "MSAA" // Settings::MSAA.name
  var MSAA by PreferenceDelegate(0, Hex.launchPreference, true) { it !in 0..16 }

  var enableGLDebugging by PreferenceDelegate(
    false,
    onChange = { _, old, new ->
      if (new != old) {
        if (new) {
          GLProfilerRenderer.enable()
        } else {
          GLProfilerRenderer.disable()
        }
      }

      return@PreferenceDelegate new
    }
  )

  private const val DELETE_ALL_PROGRESS_STRING = "delete all"
  var deleteAllProgress by PreferenceDelegate(
    "Type '$DELETE_ALL_PROGRESS_STRING' to confirm",
    onChange = { delegate, _, new ->
      if (!delegate.initOnChange && new.equals(DELETE_ALL_PROGRESS_STRING, true)) {
        resetAllIslandProgress()
        MessagesRenderer.publishWarning("All progress have been deleted")
        Hex.screen = LevelSelectScreen
        return@PreferenceDelegate delegate.initialValue
      }
      return@PreferenceDelegate new
    }
  )

  private fun updateForegroundFPS() {
    val maxFps = if (limitFps) targetFps else 9999
    Gdx.graphics.setForegroundFPS(maxFps)
    Gdx.app.debug("SETTINGS", "New max frame rate is: $maxFps")
  }
}
