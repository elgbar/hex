package no.elg.hex.ai

import no.elg.hex.hexagon.Team

/**
 * @author Elg
 */
enum class Difficulty(val aiConstructor: (Team) -> AI?) {

  PLAYER({ null }),
  UN_LOSABLE({ NOOPAI(it) }),
  EASY({ RandomAI(it) }),
  HARD({ NotAsRandomAI(it) })
}
