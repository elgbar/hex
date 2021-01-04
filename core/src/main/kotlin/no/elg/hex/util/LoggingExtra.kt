package no.elg.hex.util

import com.badlogic.gdx.Application
import com.badlogic.gdx.Application.LOG_DEBUG
import com.badlogic.gdx.Application.LOG_INFO
import com.badlogic.gdx.ApplicationLogger
import com.badlogic.gdx.Gdx

const val LOG_TRACE = LOG_DEBUG + 1

fun ApplicationLogger.trace(tag: String, message: String) {
  debug(tag, message)
}

fun ApplicationLogger.trace(tag: String, message: String, exception: Throwable) {
  if (Gdx.app.logLevel >= LOG_TRACE) {
    debug(tag, message)
    exception.printStackTrace(System.out)
  }
}

fun Application.trace(tag: String, message: String) {
  if (logLevel >= LOG_TRACE) applicationLogger.trace(tag, message)
}

fun Application.trace(tag: String, message: () -> String) {
  if (logLevel >= LOG_TRACE) applicationLogger.trace(tag, message())
}

fun Application.trace(tag: String, message: String, exception: Throwable) {
  if (logLevel >= LOG_TRACE) applicationLogger.trace(tag, message, exception)
}

fun Application.debug(tag: String, message: () -> String) {
  if (logLevel >= LOG_DEBUG) applicationLogger.debug(tag, message())
}

fun Application.log(tag: String, message: () -> String) {
  if (logLevel >= LOG_INFO) applicationLogger.debug(tag, message())
}
