package no.elg.hex.input

import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys.ENTER
import com.badlogic.gdx.InputAdapter
import no.elg.hex.hexagon.BARON_STRENGTH
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.strengthToType
import no.elg.hex.island.Hand
import no.elg.hex.island.Island.Companion.PLAYER_TEAM
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData

/**
 * @author Elg
 */
class GameInputProcessor(private val islandScreen: IslandScreen) : InputAdapter() {


  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    with(islandScreen) {
      when (button) {
        Buttons.LEFT -> {
          val cursorHex = basicInputProcessor.cursorHex ?: return false
          val cursorHexData = cursorHex.getData(island)
          val territory = island.selected

          if ((territory == null || !territory.hexagons.contains(cursorHex)) && cursorHexData.team == PLAYER_TEAM) {
            island.select(cursorHex)
          }

          val cursorPiece = cursorHexData.piece
          val oldTeam = cursorHexData.team

          val hand = island.inHand
          if (hand == null) {
            if (cursorPiece.movable && cursorPiece is LivingPiece && !cursorPiece.moved && cursorHexData.team == PLAYER_TEAM) {
              //We currently don't hold anything in our hand, so pick it up!
              island.inHand = Hand(PLAYER_TEAM, cursorPiece, cursorHex)
              cursorHexData.setPiece(Empty::class)
            }
            return true
          }

          val (piece, moved) = if (cursorPiece is LivingPiece && hand.piece is LivingPiece && cursorHexData.team == hand.team) {
            //merge cursor piece with held piece
            val newStr = hand.piece.strength + cursorPiece.strength
            if (newStr > BARON_STRENGTH) return true //cannot merge
            //The piece can only move when both the piece in hand and the hex pointed at has not moved
            strengthToType(newStr) to (!hand.piece.moved || !cursorPiece.moved)
          } else {
            hand.piece::class to (oldTeam != PLAYER_TEAM || cursorHexData.piece !is Empty)
          }

          if (cursorHexData.setPiece(piece)) {
            cursorHexData.team = hand.team
            island.inHand = null
            val newPiece = cursorHexData.piece
            if (newPiece is LivingPiece) {
              newPiece.moved = moved
            }
          }
        }
        else -> return false
      }
    }
    return true
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      ENTER -> islandScreen.island.endTurn()
      else -> return false
    }
    return true
  }

}
