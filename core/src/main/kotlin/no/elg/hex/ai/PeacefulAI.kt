package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import no.elg.hex.input.GameInteraction
import no.elg.hex.island.Island

class PeacefulAI(override val team: Team) : AI {
  override suspend fun action(island: Island, gameInteraction: GameInteraction) = Unit
}