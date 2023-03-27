package no.elg.hex.platform.android

import android.os.Bundle
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

    val args = arrayOf(resources.getString(R.string.args))

    Hex.args = ArgParser(args).parseInto(::ApplicationArgumentsParser)
    Hex.launchPreference = AndroidPreferences(getSharedPreferences(Hex.LAUNCH_PREF, MODE_PRIVATE))
    Hex.platform = AndroidPlatform(resources)

    if (Hex.launchPreference.contains(Settings.MSAA_SAMPLES_PATH)) {
      config.numSamples = Hex.launchPreference.getInteger(Settings.MSAA_SAMPLES_PATH)
    } else {
      config.numSamples = 2 //default value
    }

    config.depth = 0
    config.useImmersiveMode = false
    config.useCompass = false
    config.useAccelerometer = false
    config.useGyroscope = false
    config.useRotationVectorSensor = false
    config.disableAudio = Hex.launchPreference.getBoolean(Settings.DISABLE_AUDIO_PATH)
    Hex.audioDisabled = config.disableAudio
    initialize(Hex, config)
  }
}
