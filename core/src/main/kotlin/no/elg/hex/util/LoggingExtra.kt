package no.elg.hex.util

import com.badlogic.gdx.Application
import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Application.LOG_ERROR
import com.badlogic.gdx.Application.LOG_INFO
import com.badlogic.gdx.Application.LOG_NONE
import no.elg.hex.Hex

const val LOG_TRACE = LOG_DEBUG + 1

fun logLevelToName(level: Int): String =
  when (level) {
    LOG_TRACE -> "trace"
    LOG_DEBUG -> "debug"
    LOG_INFO -> "info"
    LOG_ERROR -> "error"
    LOG_NONE -> "none"
    else -> "Unknown (given: $level)"
  }

val Application.tracingEnabled: Boolean get() = logLevel >= LOG_TRACE
fun Application.trace(tag: String, exception: Throwable? = null, message: () -> String) {
  if (tracingEnabled) Hex.platform.trace(tag, exception, message())
}

val Application.debugEnabled: Boolean get() = logLevel >= LOG_DEBUG
fun Application.debug(tag: String, message: () -> String) {
  if (debugEnabled) applicationLogger.debug(tag, message())
}

val Application.infoEnabled: Boolean get() = logLevel >= LOG_INFO
fun Application.info(tag: String, message: () -> String) {
  if (infoEnabled) applicationLogger.log(tag, message())
}

val Application.errorEnabled: Boolean get() = logLevel >= LOG_ERROR
fun Application.error(tag: String, message: () -> String) {
  if (errorEnabled) applicationLogger.error(tag, message())
}