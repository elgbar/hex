package no.elg.hex.screens

import no.elg.hex.Hex
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.input.BasicIslandInputProcessor
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island

/** @author Elg */
class PlayableIslandScreen(id: Int, island: Island) : PreviewIslandScreen(id, island) {

  val basicIslandInputProcessor: BasicIslandInputProcessor by lazy {
    BasicIslandInputProcessor(this)
  }
  val inputProcessor by lazy { GameInputProcessor(this) }

  private val frameUpdatable by lazy { GameInfoRenderer(this) }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  override fun render(delta: Float) {
    super.render(delta)
    if (Hex.debug) {
      debugRenderer.frameUpdate()
    }
    frameUpdatable.frameUpdate()
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.addProcessor(inputProcessor)

    if (island.currentAI != null) {
      island.endTurn(inputProcessor)
    }
  }

  override fun dispose() {
    super.dispose()
    Hex.inputMultiplexer.removeProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.removeProcessor(inputProcessor)
  }
}
