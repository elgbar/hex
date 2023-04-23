package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Timer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** @author Elg */
fun schedule(delaySeconds: Float, task: () -> Unit) {
  val realTask =
    object : Timer.Task() {
      override fun run() = task()
    }
  Timer.schedule(realTask, delaySeconds)
}


/**
 * Executes the given [block] and returns elapsed time in milliseconds.
 *
 * @sample samples.system.Timing.measureBlockTimeMillis
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> returningMeasureTimeMillis(block: () -> T): Pair<T, Long> {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val start = System.currentTimeMillis()
  val returned = block()
  return returned to System.currentTimeMillis() - start
}

@OptIn(ExperimentalContracts::class)
inline fun <T> reportTiming(actionName: String, minSignificantTimeMs: Long = 10L, action: () -> T): T {
  contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
  val (returned, time) = returningMeasureTimeMillis(action)
  if (time > minSignificantTimeMs) {
    Gdx.app.debug("TIME") { "Took ${time / 1000f} seconds to $actionName" }
  } else {
    Gdx.app.trace("TIME") { "Took ${time / 1000f} seconds to $actionName" }
  }
  return returned
}