package no.elg.hex.input

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Input.Keys.BACKSPACE
import com.badlogic.gdx.Input.Keys.ESCAPE
import com.badlogic.gdx.Input.Keys.F10
import com.badlogic.gdx.Input.Keys.F11
import com.badlogic.gdx.Input.Keys.F12
import com.badlogic.gdx.Input.Keys.SPACE
import com.badlogic.gdx.Input.Keys.Y
import com.badlogic.gdx.Input.Keys.Z
import no.elg.hex.Hex
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.HexagonData.Companion.EDGE_DATA
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Spearman
import no.elg.hex.screens.PlayableIslandScreen
import no.elg.hex.util.getData
import no.elg.hex.util.isKeyPressed
import no.elg.hex.util.saveProgress
import no.elg.hex.util.toggleShown

/** @author Elg */
class GameInputProcessor(val screen: PlayableIslandScreen) : AbstractInput(true) {

  private val gameInteraction: GameInteraction get() = screen.island.gameInteraction

  private fun userClick(longPress: Boolean): Boolean {
    if (screen.isDisposed || screen.island.isCurrentTeamAI()) {
      return false
    }
    val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false
    return gameInteraction.click(cursorHex, longPress).also { result ->
      if (result) {
        screen.saveProgress()
      }
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    if (screen.island.isCurrentTeamAI()) return false

    when (keycode) {
      BACKSPACE, SPACE, ESCAPE -> {
        when {
          screen.island.hand != null -> screen.island.hand = null
          keycode != ESCAPE && screen.island.selected != null -> screen.island.select(null)
          else -> return false
        }
      }

      F12 -> if (Hex.debug || Hex.args.cheating) gameInteraction.cheating = !gameInteraction.cheating
      F11 -> if (gameInteraction.cheating) screen.acceptAISurrender.toggleShown(screen.stage)
      F10 -> if (gameInteraction.cheating) {
        screen.island.selected?.hexagons?.forEach {
          val piece = screen.island.getData(it).piece
          (piece as? LivingPiece)?.moved = false
        }
      }

      Z -> if (Keys.CONTROL_LEFT.isKeyPressed() || Keys.CONTROL_RIGHT.isKeyPressed()) screen.island.history.undo()
      Y -> if (Keys.CONTROL_LEFT.isKeyPressed() || Keys.CONTROL_RIGHT.isKeyPressed()) screen.island.history.redo()

      else -> {
        if (screen.island.hand == null || screen.island.hand?.piece?.data === EDGE_DATA) {
          val piece = keycodeToPiece(keycode) ?: return false
          gameInteraction.buyUnit(piece)
        }
      }
    }
    return true
  }

  override fun longPress(x: Float, y: Float): Boolean = userClick(true)
  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
    userClick(false)
    return false
  }

  companion object {
    fun keycodeToPiece(keycode: Int): Piece? {
      return when (keycode) {
        Keys.F1 -> Castle(EDGE_DATA)
        Keys.F2 -> Peasant(EDGE_DATA)
        Keys.F3 -> Spearman(EDGE_DATA)
        Keys.F4 -> Knight(EDGE_DATA)
        Keys.F5 -> Baron(EDGE_DATA)
        else -> return null
      }
    }
  }
}