package no.elg.hex.ai

import no.elg.hex.hexagon.Team
import java.lang.Integer.MAX_VALUE

/**
 * @author Elg
 */
enum class Difficulty(val aiConstructor: (Team) -> AI?) {

  PLAYER({ null }),
  EASY({ NotAsRandomAI(it, MAX_VALUE, 0.1f) }),
  NORMAL({ NotAsRandomAI(it, 5, 0.01f) }),
  HARD({ NotAsRandomAI(it, 0, 0.0001f) })
}