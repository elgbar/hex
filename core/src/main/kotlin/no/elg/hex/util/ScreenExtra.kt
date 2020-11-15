package no.elg.hex.util

import java.awt.DisplayMode
import java.awt.GraphicsEnvironment

val defaultDisplayMode: DisplayMode =
  GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode
