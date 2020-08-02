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
          if (cursorHexData.team != PLAYER_TEAM) return false
          val territory = island.selected

          if (territory == null || !territory.hexagons.contains(cursorHex)) {
            island.select(cursorHex)
          }

          val cursorPiece = cursorHexData.piece

          val hand = island.inHand
          if (hand == null) {
            if (cursorPiece.movable && cursorPiece is LivingPiece && !cursorPiece.moved) {
              //We currently don't hold anything in our hand, so pick it up!
              island.inHand = Hand(PLAYER_TEAM, cursorPiece, cursorHex)
              cursorHexData.setPiece(Empty::class)
            }
            return true
          }

          val piece = if (cursorPiece is LivingPiece && hand.piece is LivingPiece && cursorHexData.team == hand.team) {
            //merge cursor piece with held piece
            val newStr = hand.piece.strength + cursorPiece.strength
            if (newStr > BARON_STRENGTH) return true //cannot merge
            strengthToType(newStr)
          } else {
            hand.piece::class
          }

          val new = cursorHexData.setPiece(piece)
          if (new !== cursorPiece) {
            island.inHand = null
            if (cursorPiece is Empty && new is LivingPiece) {
              new.moved = false
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
