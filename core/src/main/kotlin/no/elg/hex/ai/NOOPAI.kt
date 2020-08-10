package no.elg.hex.ai

import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Territory

/** @author Elg */
object NOOPAI : AI {
  override fun action(territory: Territory, gameInputProcessor: GameInputProcessor) {
    // NOP
  }
}
