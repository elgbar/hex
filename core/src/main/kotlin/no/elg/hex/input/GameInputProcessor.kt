package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Input.Keys.ENTER
import com.badlogic.gdx.InputAdapter
import kotlin.reflect.full.isSubclassOf
import no.elg.hex.Hex
import no.elg.hex.hexagon.BARON_STRENGTH
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Spearman
import no.elg.hex.hexagon.strengthToType
import no.elg.hex.island.Hand
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.canAttack
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import org.hexworks.mixite.core.api.Hexagon

/** @author Elg */
class GameInputProcessor(private val islandScreen: IslandScreen) : InputAdapter() {

  var infiniteMoney = Hex.args.cheating

  fun pickUp(island: Island, cursorHexData: HexagonData, territory: Territory) {
    val cursorPiece = cursorHexData.piece
    if (cursorPiece.movable &&
        cursorPiece is LivingPiece &&
        !cursorPiece.moved &&
        cursorHexData.team == island.currentTeam) {
      // We currently don't hold anything in our hand, so pick it up!
      island.inHand = Hand(territory, cursorPiece)
      cursorHexData.setPiece(Empty::class)
    }
    Gdx.app.debug("PLACE", "Hand was null, now it is ${island.inHand}")
  }

  fun placeDown(
      island: Island,
      territory: Territory,
      placeOn: Hexagon<HexagonData>,
      newPiece: Piece,
      oldTerritory: Territory?
  ) {
    val hexData = island.getData(placeOn)
    if (hexData.team != island.currentTeam && !territory.enemyBorderHexes.contains(placeOn)) {
      Gdx.app.debug("PLACE", "Tried to place piece on enemy hex outside border hexes")
      return
    }

    val oldPiece = hexData.piece

    val (newPieceType, moved) = if (oldPiece is LivingPiece &&
        newPiece is LivingPiece &&
        hexData.team == territory.team) {
      // merge cursor piece with held piece
      val newStr = newPiece.strength + oldPiece.strength
      if (newStr > BARON_STRENGTH) return // cannot merge
      // The piece can only move when both the piece in hand and the hex pointed at has not moved
      strengthToType(newStr) to (newPiece.moved || oldPiece.moved)
    } else {
      newPiece::class to (hexData.team != island.currentTeam || oldPiece !is Empty)
    }

    if (newPieceType.isSubclassOf(LivingPiece::class)) {
      if (hexData.team == territory.team && (oldPiece is Capital || oldPiece is Castle)) {
        Gdx.app
            .debug(
                "PLACE",
                "Cannot place a living entity of the same team onto a capital or castle piece")
        return
      } else if (hexData.team != territory.team && !island.canAttack(placeOn, newPiece)) {
        Gdx.app
            .debug(
                "PLACE",
                "Cannot attack ${oldPiece::class.simpleName} with a ${newPiece::class.simpleName}")
        return
      }
    } else if (Castle::class != newPieceType) {
      throw IllegalStateException(
          "Holding illegal piece '$newPieceType', can only hold living pieces and castle!")
    }

    if (hexData.setPiece(newPieceType)) {
      hexData.team = territory.team

      island.inHand?.holding = oldTerritory != territory
      val updatedPiece = hexData.piece
      if (updatedPiece is LivingPiece) {
        updatedPiece.moved = moved
      }
      for (neighbor in island.getNeighbors(placeOn)) {
        island.select(neighbor)
      }
      // reselect territory to update it's values
      island.select(placeOn)
    }
  }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (islandScreen.island.currentTeam != Island.STARTING_TEAM) return false
    with(islandScreen) {
      when (button) {
        Buttons.LEFT -> {
          val cursorHex = basicIslandInputProcessor.cursorHex ?: return false
          val cursorHexData = island.getData(cursorHex)

          val oldTerritory = island.selected
          if ((oldTerritory == null || !oldTerritory.hexagons.contains(cursorHex)) &&
              cursorHexData.team == island.currentTeam) {
            island.select(cursorHex)
          }
          val territory = island.selected
          if (territory == null) {
            Gdx.app.debug("PLACE", "Territory is still null after selecting it")
            return true
          }

          val hand = island.inHand
          if (hand == null) {
            pickUp(island, cursorHexData, territory)
          } else {
            placeDown(island, territory, cursorHex, hand.piece, oldTerritory)
          }
        }
        else -> return false
      }
    }
    return true
  }

  override fun keyDown(keycode: Int): Boolean {
    if (islandScreen.island.currentTeam != Island.STARTING_TEAM) return false
    fun setIfTerritorySelected(piece: Piece) {
      islandScreen.island.selected?.also {
        if (!infiniteMoney) {
          if (it.capital.balance < piece.price) {
            return@also
          }
          it.capital.balance -= piece.price
        }
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
      Keys.F12 -> if (Hex.args.debug) infiniteMoney = !infiniteMoney
      else -> return false
    }
    return true
  }
}
