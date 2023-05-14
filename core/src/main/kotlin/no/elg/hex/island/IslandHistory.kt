package no.elg.hex.island

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.model.IslandDto
import no.elg.hex.util.debug

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
    private set
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
  fun remember(note: String, event: () -> Unit) {
    ignore(event)
    remember(note)
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
  fun ignore(event: () -> Unit) {
    val wasRemembering = enabled
    enabled = false
    event()
    enabled = wasRemembering // only re-enable if it was enabled before
  }

  fun canUndo(): Boolean = (historyPointer + 1) in history.indices
  fun canRedo(): Boolean = (historyPointer - 1) in history.indices

  fun undo() {
    Hex.assets.undoSound?.play(Settings.volume)
    changeHistory(historyPointer + 1, "un")
  }

  fun redo() {
    Hex.assets.undoSound?.play(Settings.volume)
    changeHistory(historyPointer - 1, "re")
  }

  fun undoAll() {
    Hex.assets.undoAllSound?.play(Settings.volume)
    changeHistory(history.size - 1, "un")
  }

  fun redoAll() {
    Hex.assets.undoAllSound?.play(Settings.volume)
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