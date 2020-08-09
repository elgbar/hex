package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Input.Keys.ENTER
import com.badlogic.gdx.InputAdapter
import no.elg.hex.hexagon.BARON_STRENGTH
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.KNIGHT_STRENGTH
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Spearman
import no.elg.hex.hexagon.strengthToType
import no.elg.hex.island.Hand
import no.elg.hex.island.Island.Companion.PLAYER_TEAM
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import kotlin.math.min
import kotlin.reflect.full.isSubclassOf

/**
 * @author Elg
 */
class GameInputProcessor(private val islandScreen: IslandScreen) : InputAdapter() {


  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    with(islandScreen) {
      when (button) {
        Buttons.LEFT -> {
          val cursorHex = basicIslandInputProcessor.cursorHex ?: return false
          val cursorHexData = island.getData(cursorHex)
          val territory = island.selected

          if ((territory == null || !territory.hexagons.contains(cursorHex)) && cursorHexData.team == PLAYER_TEAM) {
            island.select(cursorHex)
          }

          val cursorPiece = cursorHexData.piece

          val hand = island.inHand
          if (hand == null) {
            if (territory != null && cursorPiece.movable && cursorPiece is LivingPiece && !cursorPiece.moved && cursorHexData.team == PLAYER_TEAM) {
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

          if (newPieceType.isSubclassOf(LivingPiece::class)) {
            val cursorStrength = island.calculateStrength(cursorHex)
            if (cursorHexData.team == hand.territory.team && (cursorPiece is Capital || cursorPiece is Castle)) {
              Gdx.app.debug("PLACE", "Cannot place a living entity of the same team onto a capital or castle piece")
              return true
            } else if (cursorHexData.team != hand.territory.team && handPiece.strength <= min(cursorStrength, KNIGHT_STRENGTH)) {
              Gdx.app.debug("PLACE", "Cannot attack ${cursorPiece::class.simpleName} with a ${this::class.simpleName}")
              return true
            }
          } else if (Castle::class != newPieceType) {
            throw IllegalStateException("Holding illegal piece '$newPieceType', can only hold living pieces and castle!")
          }

          if (cursorHexData.setPiece(newPieceType)) {
            cursorHexData.team = hand.territory.team
            island.inHand?.holding = false
            val newPiece = cursorHexData.piece
            if (newPiece is LivingPiece) {
              newPiece.moved = moved
            }
            for (neighbor in island.getNeighbors(cursorHex)) {
              island.select(neighbor)
            }
            //reselect territory to update it's values
            island.select(cursorHex)
          }
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
        if (piece is LivingPiece) piece.moved = false
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
