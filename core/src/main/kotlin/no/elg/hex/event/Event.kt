@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")

package no.elg.hex.event

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import com.badlogic.gdx.utils.Array as GdxUtilsArray

annotation class EventListenerList

sealed class Event

class TeamEndTurnEvent(val old: Team, val new: Team) : Event() {
  init {
    require(old !== new) { "There is no team change if the old and the new team are the same!" }
  }

  companion object {
    @EventListenerList
    internal val listeners = GdxUtilsArray<(TeamEndTurnEvent) -> Unit>()
  }
}

sealed class HexagonDataEvent(val data: HexagonData) : Event()

/**
 * Called when the team of a [HexagonData] will be changed. Event is called after the change occur
 */
class HexagonChangedTeamEvent(data: HexagonData, val old: Team, val new: Team) : HexagonDataEvent(data) {

  init {
    require(old !== new) { "There is no team change if the old and the new team are the same!" }
  }

  companion object {
    @EventListenerList
    internal val listeners = GdxUtilsArray<(HexagonChangedTeamEvent) -> Unit>()
  }
}