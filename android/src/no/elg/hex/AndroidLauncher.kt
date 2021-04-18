package no.elg.hex

import android.content.SharedPreferences
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidPreferences
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences
import com.xenomachina.argparser.ArgParser
import no.elg.hex.Settings.MSAA_SAMPLES_PATH

class AndroidLauncher : AndroidApplication() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val config = AndroidApplicationConfiguration()

//    val args = arrayOf<String>("--debug")
    val args = arrayOf<String>()

    Hex.args = ArgParser(args).parseInto(::ApplicationArgumentsParser)
    Hex.launchPreference = AndroidPreferences(getSharedPreferences(Hex.LAUNCH_PREF, MODE_PRIVATE))


    if (Hex.launchPreference.contains(MSAA_SAMPLES_PATH)) {
      config.numSamples = Hex.launchPreference.getInteger(MSAA_SAMPLES_PATH)
    } else {
      config.numSamples = 2 //default value
    }

    config.hideStatusBar = true
//    config.useImmersiveMode = true
    config.depth = 0
    config.useCompass = false
    config.useAccelerometer = false
    config.useGyroscope = false
    config.useRotationVectorSensor = false
    config.disableAudio = Hex.launchPreference.getBoolean(Settings.SOUND_ENABLED_PATH)
    initialize(Hex, config)
  }
}
