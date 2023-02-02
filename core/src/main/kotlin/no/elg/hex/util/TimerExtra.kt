package no.elg.hex.util

import com.badlogic.gdx.utils.Timer

/** @author Elg */
fun schedule(delaySeconds: Float, task: () -> Unit) {
  val realTask =
    object : Timer.Task() {
      override fun run() = task()
    }
  Timer.schedule(realTask, delaySeconds)
}