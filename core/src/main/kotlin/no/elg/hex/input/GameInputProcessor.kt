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
              val territory = island.selected ?: return true
              //We currently don't hold anything in our hand, so pick it up!
              island.inHand = Hand(territory, cursorPiece)
              cursorHexData.setPiece(Empty::class)
            }
            return true
          }

          val handPiece = hand.piece

          val (newPieceType, moved) = if (cursorPiece is LivingPiece && handPiece is LivingPiece && cursorHexData.team == hand.territory.team) {
            //merge cursor piece with held piece
            val newStr = handPiece.strength + cursorPiece.strength
            if (newStr > BARON_STRENGTH) return true //cannot merge
            //The piece can only move when both the piece in hand and the hex pointed at has not moved
            strengthToType(newStr) to (handPiece.moved || cursorPiece.moved)
          } else {
            handPiece::class to (cursorHexData.team != PLAYER_TEAM || cursorPiece !is Empty)
          }

          if (cursorHexData.setPiece(newPieceType)) {
            cursorHexData.team = hand.territory.team
            island.inHand = null
            val newPiece = cursorHexData.piece
            if (newPiece is LivingPiece) {
              newPiece.moved = moved
            }
          }
          //reselect territory to update it's values
          island.select(cursorHex)
        }
        else -> return false
      }
    }
    return true
  }

  override fun keyDown(keycode: Int): Boolean {
    fun setIfTerritorySelected(piece: Piece) {
      islandScreen.island.selected?.also {
        it.capital.balance -= piece.price
        islandScreen.island.inHand = Hand(it, piece)
      }
    }

    when (keycode) {
      ENTER -> islandScreen.island.endTurn()
      Keys.F1 -> setIfTerritorySelected(Castle(HexagonData.EDGE_DATA))
      Keys.F2 -> setIfTerritorySelected(Peasant(HexagonData.EDGE_DATA))
      Keys.F3 -> setIfTerritorySelected(Spearman(HexagonData.EDGE_DATA))
      Keys.F4 -> setIfTerritorySelected(Knight(HexagonData.EDGE_DATA))
      Keys.F5 -> setIfTerritorySelected(Baron(HexagonData.EDGE_DATA))
      else -> return false
    }
    return true
  }

}
