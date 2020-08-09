package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.util.getTerritories

/**
 * @author Elg
 */
interface AI {

  fun action(territory: Territory)
}

fun AI.actionOnAll(island: Island, team: Team) {
  for (territory in island.getTerritories(team)) {
    action(territory)
  }
}
