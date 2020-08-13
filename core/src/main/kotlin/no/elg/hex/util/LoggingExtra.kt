package no.elg.hex.util

import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationLogger

const val LOG_TRACE = Application.LOG_DEBUG + 1

fun ApplicationLogger.trace(tag: String, message: String) {
  println("[$tag] $message")
}

fun ApplicationLogger.trace(tag: String, message: String, exception: Throwable) {
  println("[$tag] $message")
  exception.printStackTrace(System.out)
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
