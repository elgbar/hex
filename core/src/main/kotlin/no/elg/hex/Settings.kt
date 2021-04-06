package no.elg.hex

import com.badlogic.gdx.Gdx
import no.elg.hex.util.delegate.PreferenceDelegate

object Settings {

  var confirmEndTurn by PreferenceDelegate(true)
  var confirmSurrender by PreferenceDelegate(true)

  var showFps by PreferenceDelegate(false)
  var limitFps by PreferenceDelegate(false, onChange = { _, _ -> updateForegroundFPS() })
  var targetFps by PreferenceDelegate(30, onChange = { _, _ -> updateForegroundFPS() }) { it < 5 }

  const val MSAA_SAMPLES_PATH = "MSAA" // Settings::MSAA.name
  var MSAA by PreferenceDelegate(0, Hex.launchPreference, true) { it !in 0..16 }

  private fun updateForegroundFPS() {
    val maxFps = if (limitFps) targetFps else 9999
    Gdx.graphics.setForegroundFPS(maxFps)
    Gdx.app.debug("SETTINGS", "New max frame rate is: $maxFps")
  }
}
