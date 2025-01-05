package no.elg.hex.island

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.model.IslandDto
import no.elg.hex.util.debug
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author Elg
 */
class IslandHistory(val island: Island) {

  private val history = ArrayDeque<IslandDto>()
  val historyNotes = ArrayDeque<String>()
  var historyPointer = 0
    private set

  /**
   * If [remember] will record any remember request.
   */
  var enabled = true
  val disabled get() = !enabled

  /**
   * Clear current history
   */
  fun clear() {
    historyNotes.clear()
    history.clear()
    historyPointer = 0
    remember("Initial state")
  }

  fun enable() {
    enabled = true
  }

  fun disable() {
    enabled = false
  }

  /**
   * Record the events within the [event] scope as a single event. Will call [remember] after [event]
   */
  inline fun <R> remember(note: String, event: () -> R): R {
    contract { callsInPlace(event, InvocationKind.EXACTLY_ONCE) }
    return ignore(event).also {
      remember(note)
    }
  }

  fun remember(note: String) {
    if (enabled) {
      if (historyPointer != 0) {
        for (i in 0 until historyPointer) {
          history.removeFirst()
          historyNotes.removeFirst()
        }
        historyPointer = 0
      }
      history.addFirst(island.createDto())
      historyNotes.addFirst(note)
    }
  }

  /**
   * Ignore anything that happens within [event]
   */
  inline fun <R> ignore(event: () -> R): R {
    contract { callsInPlace(event, InvocationKind.EXACTLY_ONCE) }
    val wasRemembering = enabled
    enabled = false
    return event().also {
      enabled = wasRemembering // only re-enable if it was enabled before
    }
  }

  fun canUndo(): Boolean = (historyPointer + 1) in history.indices
  fun canRedo(): Boolean = (historyPointer - 1) in history.indices

  fun undo() {
    Hex.assets.undoSound?.play(Settings.masterVolume)
    changeHistory(historyPointer + 1, "un")
  }

  fun redo() {
    Hex.assets.undoSound?.play(Settings.masterVolume)
    changeHistory(historyPointer - 1, "re")
  }

  fun undoAll() {
    Hex.assets.undoAllSound?.play(Settings.masterVolume)
    changeHistory(history.size - 1, "un")
  }

  fun redoAll() {
    Hex.assets.undoAllSound?.play(Settings.masterVolume)
    changeHistory(0, "re")
  }

  private fun changeHistory(pointer: Int, prefix: String) {
    if (pointer !in history.indices) {
      Gdx.app.debug("HISTORY") { "Nothing left to ${prefix}do" }
      return
    }
    Gdx.app.debug("HISTORY") { "${prefix}do ${historyNotes[historyPointer]}" }
    val wasEnabled = enabled
    enabled = false
    historyPointer = pointer
    island.restoreState(history[pointer].copy())
    enabled = wasEnabled
  }
}