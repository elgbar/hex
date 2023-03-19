package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import java.lang.Integer.MAX_VALUE

/**
 * @author Elg
 */
enum class Difficulty(val aiConstructor: (Team) -> AI?) {

  PLAYER({ null }),
  EASY({ NotAsRandomAI(it, MAX_VALUE) }),
  NORMAL({ NotAsRandomAI(it, 5) }),
  HARD({ NotAsRandomAI(it, 0) })
}