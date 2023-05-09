package no.elg.hex.util

import com.badlogic.gdx.Application
import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Application.LOG_ERROR
import com.badlogic.gdx.Application.LOG_INFO
import com.badlogic.gdx.Application.LOG_NONE
import no.elg.hex.Hex

const val LOG_TRACE = LOG_DEBUG + 1

fun logLevelToName(level: Int): String {
  return when (level) {
    LOG_TRACE -> "trace"
    LOG_DEBUG -> "debug"
    LOG_INFO -> "info"
    LOG_ERROR -> "error"
    LOG_NONE -> "none"
    else -> "Unknown (given: $level)"
  }
}

fun Application.trace(tag: String, exception: Throwable? = null, message: () -> String) {
  if (logLevel >= LOG_TRACE) Hex.platform.trace(tag, exception, message())
}

fun Application.debug(tag: String, message: () -> String) {
  if (logLevel >= LOG_DEBUG) applicationLogger.debug(tag, message())
}

fun Application.log(tag: String, message: () -> String) = info(tag, message)
fun Application.info(tag: String, message: () -> String) {
  if (logLevel >= LOG_INFO) applicationLogger.log(tag, message())
}

fun Application.error(tag: String, message: () -> String) {
  if (logLevel >= LOG_ERROR) applicationLogger.error(tag, message())
}