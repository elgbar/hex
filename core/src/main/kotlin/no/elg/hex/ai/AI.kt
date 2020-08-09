package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.util.getTerritories

/** @author Elg */
interface AI {

  fun action(territory: Territory, gameInputProcessor: GameInputProcessor)
}

fun AI.actionOnAll(island: Island, team: Team, gameInputProcessor: GameInputProcessor) {
  for (territory in island.getTerritories(team)) {
    action(territory, gameInputProcessor)
  }
}
