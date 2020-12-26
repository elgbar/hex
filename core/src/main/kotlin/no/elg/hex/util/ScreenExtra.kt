package no.elg.hex.util

import java.awt.DisplayMode
import java.awt.GraphicsEnvironment

val defaultDisplayMode: DisplayMode by lazy { GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode }
val defaultDisplayWidth: Int by lazy { defaultDisplayMode.width }
val defaultDisplayHeight: Int by lazy { defaultDisplayMode.height }
