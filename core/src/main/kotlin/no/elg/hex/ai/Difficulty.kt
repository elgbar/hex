package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import java.lang.Integer.MAX_VALUE

/**
 * @author Elg
 */
enum class Difficulty(val aiConstructor: (Team) -> AI?) {

  PLAYER({ null }),
  PEACEFUL({ PeacefulAI(it) }),
  EASY({ NotAsRandomAI(it, MAX_VALUE, 0.1, 0.0) }),
  NORMAL({ NotAsRandomAI(it, 8, 0.01, 0.05) }),
  HARD({ NotAsRandomAI(it, 1, 0.0001, 0.1) }),
}