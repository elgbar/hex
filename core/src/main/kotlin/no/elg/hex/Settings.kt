package no.elg.hex

import no.elg.hex.util.delegate.PreferenceDelegate

object Settings {

  var confirmEndTurn by PreferenceDelegate(true)
  var confirmSurrender by PreferenceDelegate(true)

  var showFps by PreferenceDelegate(false)
  var limitFps by PreferenceDelegate(false)
  var targetFps by PreferenceDelegate(30) { it <= 0 }
}
