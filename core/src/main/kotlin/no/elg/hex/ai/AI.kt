package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import no.elg.hex.input.GameInteraction
import no.elg.hex.island.Island

/** @author Elg */
interface AI {

  val team: Team

  /**
   * Do the AI's turn
   *
   * @return If the AI is still alive, i.e., `false` if the AI is dead
   */
  suspend fun action(island: Island, gameIntegration: GameInteraction): Boolean
}