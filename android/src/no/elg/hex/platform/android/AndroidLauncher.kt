package no.elg.hex.platform.android

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidPreferences
import com.xenomachina.argparser.ArgParser
import no.elg.hex.ApplicationArgumentsParser
import no.elg.hex.Hex
import no.elg.hex.R
import no.elg.hex.Settings


class AndroidLauncher : AndroidApplication() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val config = AndroidApplicationConfiguration()

    val args = resources.getString(R.string.args).split(' ').filter { it.isNotBlank() }.toTypedArray()
    Hex.args = ArgParser(args).parseInto(::ApplicationArgumentsParser)
    if (Hex.debug) {
      StrictMode.enableDefaults()
    }

    Hex.launchPreference = AndroidPreferences(getSharedPreferences(Hex.LAUNCH_PREF, MODE_PRIVATE))
    Hex.platform = AndroidPlatform(this)
    config.numSamples = 2 //default value

    config.depth = 0

    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
    val configurationInfo = activityManager?.deviceConfigurationInfo
    val glVersion = configurationInfo?.glEsVersion?.toFloatOrNull() ?: 2f

    if (glVersion >= 3) {
      config.useGL30 = true
    }
    config.useImmersiveMode = false
    config.useCompass = false
    config.useAccelerometer = false
    config.useGyroscope = false
    config.useRotationVectorSensor = false
    config.disableAudio = false

    Hex.audioDisabled = config.disableAudio
    initialize(Hex, config)
  }
}
