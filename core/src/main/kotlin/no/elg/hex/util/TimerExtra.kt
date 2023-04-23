package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Timer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.system.measureTimeMillis

/** @author Elg */
fun schedule(delaySeconds: Float, task: () -> Unit) {
  val realTask =
    object : Timer.Task() {
      override fun run() = task()
    }
  Timer.schedule(realTask, delaySeconds)
}

@OptIn(ExperimentalContracts::class)
inline fun reportTiming(actionName: String, minSignificantTimeMs: Long = 10L, action: () -> Unit) {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  val time = measureTimeMillis(action)
  if (time > minSignificantTimeMs) {
    Gdx.app.debug("TIME") { "Took ${time / 1000f} seconds to $actionName" }
  } else {
    Gdx.app.trace("TIME") { "Took ${time / 1000f} seconds to $actionName" }
  }
}