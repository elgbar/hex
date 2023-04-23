@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")

package no.elg.hex.event

import com.badlogic.gdx.utils.Array
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import com.badlogic.gdx.utils.Array as GdxUtilsArray

open class EventListeners<T : Event>(val listeners: Array<(T) -> Unit> = GdxUtilsArray<(T) -> Unit>())

sealed interface Event

sealed interface HexagonDataEvent : Event {
  val data: HexagonData
}

data class TeamEndTurnEvent(val old: Team, val new: Team) : Event {
  init {
    require(old !== new) { "There is no team change if the old and the new team are the same!" }
  }

  companion object : EventListeners<TeamEndTurnEvent>()
}

data class HandChangedEvent(val old: Piece?, val new: Piece?) : Event {
  companion object : EventListeners<HandChangedEvent>()
}

/**
 * Called when the team of a [HexagonData] will be changed. Event is called after the change occur
 */
data class HexagonChangedTeamEvent(override val data: HexagonData, val old: Team, val new: Team) : HexagonDataEvent {
  init {
    require(old !== new) { "There is no team change if the old and the new team are the same!" }
  }

  companion object : EventListeners<HexagonChangedTeamEvent>()
}

/**
 * Called when the piece of a [HexagonData] will be changed. Event is called after the change occur
 */
data class HexagonChangedPieceEvent(override val data: HexagonData, val old: Piece, val new: Piece) : HexagonDataEvent {
  companion object : EventListeners<HexagonChangedPieceEvent>()
}

data class CapitalBalanceChanged(override val data: HexagonData, val old: Int, val new: Int) : HexagonDataEvent {
  companion object : EventListeners<CapitalBalanceChanged>()
}
