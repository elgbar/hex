package no.elg.hex.event.events

import no.elg.hex.event.ClearOnScreenChange
import no.elg.hex.event.EventListeners
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import org.hexworks.mixite.core.api.Hexagon

@ClearOnScreenChange
data class AIClickedEvent(val team: Team, val hexagon: Hexagon<HexagonData>) : Event {
  companion object : EventListeners<AIClickedEvent>()
}