package no.elg.hex

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidPreferences
import com.xenomachina.argparser.ArgParser

class AndroidLauncher : AndroidApplication() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val config = AndroidApplicationConfiguration()

//    val args = arrayOf<String>("--debug")
    val args = arrayOf<String>()

    Hex.args = ArgParser(args).parseInto(::ApplicationArgumentsParser)
    Hex.launchPreference = AndroidPreferences(getSharedPreferences(Hex.LAUNCH_PREF, MODE_PRIVATE))


    if (Hex.launchPreference.contains(Settings.MSAA_SAMPLES_PATH)) {
      config.numSamples = Hex.launchPreference.getInteger(Settings.MSAA_SAMPLES_PATH)
    } else {
      config.numSamples = 2 //default value
    }

    config.useGL30 = true
    config.depth = 0
    config.useImmersiveMode = true
    config.useCompass = false
    config.useAccelerometer = false
    config.useGyroscope = false
    config.useRotationVectorSensor = false
    config.disableAudio = Hex.launchPreference.getBoolean(Settings.DISABLE_AUDIO_PATH)
    Hex.audioDisabled = config.disableAudio
    initialize(Hex, config)
  }
}
