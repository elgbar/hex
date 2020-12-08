package no.elg.hex.island

import com.badlogic.gdx.Gdx
import no.elg.hex.island.Island.IslandDto

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
  private var rememberEnabled = true

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
    rememberEnabled = true
  }

  fun disable() {
    rememberEnabled = false
  }

  /**
   * Record the events within the [event] scope as a single event. Will call [remember] after [event]
   */
  fun remember(note: String, event: () -> Unit) {
    val wasRemembering = rememberEnabled
    rememberEnabled = false
    event()
    rememberEnabled = wasRemembering // only re-enable if it was enabled before
    remember(note)
  }

  fun remember(note: String) {
    if (rememberEnabled) {
      if (historyPointer != 0) {
        for (i in 0 until historyPointer) {
          history.removeFirst()
          historyNotes.removeFirst()
        }
        historyPointer = 0
      }
      history.addFirst(island.dto)
      historyNotes.addFirst(note)
    }
  }

  fun undo() {
    val pointer = historyPointer + 1
    if (pointer !in history.indices) {
      Gdx.app.debug("HISTORY", "Nothing left to undo")
      return
    }
    Gdx.app.debug("HISTORY", "Undo ${historyNotes[pointer - 1]}")
    historyPointer++
    rememberEnabled = false
    island.restoreState(history[pointer].copy())
    rememberEnabled = true
  }

  fun redo() {
    val pointer = historyPointer - 1
    if (pointer !in history.indices) {
      Gdx.app.debug("HISTORY", "Nothing left to redo")
      return
    }
    Gdx.app.debug("HISTORY", "Redo ${historyNotes[pointer + 1]}")
    historyPointer--
    rememberEnabled = false
    island.restoreState(history[pointer].copy())
    rememberEnabled = true
  }
}
