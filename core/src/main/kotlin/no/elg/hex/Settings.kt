package no.elg.hex

import no.elg.hex.util.delegate.PreferenceDelegate

object Settings {

  var confirmEndTurn by PreferenceDelegate(true)
  var confirmSurrender by PreferenceDelegate(true)

  var showFps by PreferenceDelegate(false)
  var limitFps by PreferenceDelegate(false, onChange = { _, _ -> updateForegroundFPS() })
  var targetFps by PreferenceDelegate(30, onChange = { _, _ -> updateForegroundFPS() }) { it < 5 }
  private fun updateForegroundFPS() {
    val maxFps = if (limitFps) targetFps else 9999
    Gdx.graphics.setForegroundFPS(maxFps)
    Gdx.app.debug("SETTINGS", "New max frame rate is: $maxFps")
  }
}
