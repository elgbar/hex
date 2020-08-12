package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island

/** @author Elg */
interface AI {

  val team: Team

  fun action(island: Island, gameInputProcessor: GameInputProcessor)
}
