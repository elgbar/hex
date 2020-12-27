package no.elg.hex

import com.badlogic.gdx.Application.ApplicationType.Android
import com.badlogic.gdx.Gdx
import no.elg.hex.util.delegate.PreferenceDelegate

object Settings {

  var confirmEndTurn by PreferenceDelegate(true)
  var confirmSurrender by PreferenceDelegate(true)
  var showFps by PreferenceDelegate(false)

  var limitFps by PreferenceDelegate(Gdx.app.type == Android)
  var targetFps by PreferenceDelegate(30)
}
