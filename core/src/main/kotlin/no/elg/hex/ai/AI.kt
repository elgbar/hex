package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import no.elg.hex.input.GameInteraction
import no.elg.hex.island.Island

/** @author Elg */
interface AI {

  val team: Team

  /**
   * Do the AI's turn
   */
  suspend fun action(island: Island, gameInteraction: GameInteraction)
}