package no.elg.hex.util

import no.elg.hex.Hex
import no.elg.hex.Settings

fun playClick() {
  Hex.assets.clickSound?.play(Settings.masterVolume)
}

fun playBadClick() {
  Hex.assets.clickBadSound?.play(Settings.masterVolume)
}

fun playMoney() {
  Hex.assets.coinsSound?.play(Settings.masterVolume)
}