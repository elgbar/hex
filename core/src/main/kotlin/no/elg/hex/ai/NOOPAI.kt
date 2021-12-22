package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island

/** @author Elg */
class NOOPAI(override val team: Team) : AI {

  override fun action(island: Island, gameInputProcessor: GameInputProcessor): Boolean {
    // NO-OP
    return false
  }
}
