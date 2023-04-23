package no.elg.hex.util

import com.kotcrab.vis.ui.widget.spinner.ArraySpinnerModel


var <T> ArraySpinnerModel<T>.value: T
  get() = this.current
  set(value) {
    this.current = value
  }