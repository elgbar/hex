package no.elg.hex

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.xenomachina.argparser.ArgParser

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val args = arrayOf<String>("--trace")
        val args = arrayOf<String>()

        Hex.args = ArgParser(args).parseInto(::ApplicationArgumentsParser)

        val config = AndroidApplicationConfiguration()
//        config.hideStatusBar = true
//        config.useImmersiveMode = true
        config.depth = 0
        config.useCompass = false
        config.useAccelerometer = false
        config.useGyroscope = false
        config.useRotationVectorSensor = false
        initialize(Hex, config)
    }
}