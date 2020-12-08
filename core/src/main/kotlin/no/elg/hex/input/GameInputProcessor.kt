package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Input.Keys.BACKSPACE
import com.badlogic.gdx.Input.Keys.ENTER
import com.badlogic.gdx.Input.Keys.SPACE
import com.badlogic.gdx.InputAdapter
import no.elg.hex.Hex
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
import no.elg.hex.screens.PlayableIslandScreen
import no.elg.hex.util.canAttack
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.isKeyPressed
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.isSubclassOf
import no.elg.hex.hexagon.HexagonData.Companion

/** @author Elg */
class GameInputProcessor(private val screen: PlayableIslandScreen) : InputAdapter() {

  var infiniteMoney = Hex.args.cheating

  private fun pickUp(island: Island, hexData: HexagonData, territory: Territory) {
    val cursorPiece = hexData.piece
    if (cursorPiece.movable &&
      cursorPiece is LivingPiece &&
      !cursorPiece.moved &&
      hexData.team == island.currentTeam
    ) {
      // We currently don't hold anything in our hand, so pick it up!
      island.history.remember("Pickup piece") {
        island.inHand = Hand(territory, cursorPiece)
        hexData.setPiece(Empty::class)
      }
      Gdx.app.trace("PLACE", "Hand was null, now it is ${island.inHand}")
    }
  }

  private fun placeDown(
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
      hexData.team == territory.team
    ) {
      // merge cursor piece with held piece
      if (newPiece.canNotMerge(oldPiece)) return
      // The piece can only move when both the piece in hand and the hex pointed at has not moved
      strengthToType(newPiece.strength + oldPiece.strength) to (newPiece.moved || oldPiece.moved)
    } else {
      newPiece::class to (hexData.team != island.currentTeam || oldPiece !is Empty)
    }

    if (newPieceType.isSubclassOf(LivingPiece::class)) {
      if (hexData.team == territory.team && (oldPiece is Capital || oldPiece is Castle)) {
        Gdx.app.debug("PLACE", "Cannot place a living entity of the same team onto a capital or castle piece")
        return
      } else if (hexData.team != territory.team && !island.canAttack(placeOn, newPiece)) {
        Gdx.app.debug("PLACE", "Cannot place castle on an enemy hex")
        return
      }
    } else if (Castle::class == newPieceType) {
      if (hexData.team != territory.team) {
        Gdx.app.debug("PLACE", "Cannot attack ${oldPiece::class.simpleName} with a ${newPiece::class.simpleName}")
        return
      }
    } else if (Castle::class != newPieceType) {
      throw IllegalStateException("Holding illegal piece '$newPieceType', can only hold living pieces and castle!")
    }

    island.history.remember("Placing piece") {
      if (hexData.setPiece(newPieceType)) {

        hexData.team = territory.team
        island.inHand?.holding = oldTerritory != territory
        val updatedPiece = hexData.piece
        if (updatedPiece is LivingPiece) {
          updatedPiece.moved = moved
        }

        for (neighbor in island.getNeighbors(placeOn)) {
          island.findTerritory(neighbor)
        }

        // reselect territory to update it's values
        island.select(placeOn)
      }
    }
  }

  fun click(hexagon: Hexagon<HexagonData>) {
    val island = screen.island
    val cursorHexData = island.getData(hexagon)

    val oldTerritory = island.selected
    if ((oldTerritory == null || !oldTerritory.hexagons.contains(hexagon)) && cursorHexData.team == island.currentTeam) {
      island.select(hexagon)
    }
    val territory = island.selected
    if (territory == null) {
      Gdx.app.debug("PLACE", "Territory is still null after selecting it")
      return
    }

    val hand = island.inHand
    if (hand == null) {
      pickUp(island, cursorHexData, territory)
    } else {
      placeDown(island, territory, hexagon, hand.piece, oldTerritory)
    }
  }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    if (screen.island.currentAI != null) return false
    when (button) {
      Buttons.LEFT -> {
        val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false
        click(cursorHex)
      }
      else -> return false
    }
    return true
  }

  override fun keyDown(keycode: Int): Boolean {
    if (screen.island.currentAI != null) return false

    when (keycode) {
      ENTER -> screen.island.endTurn(this)
      BACKSPACE, SPACE -> screen.island.inHand = null
      Keys.F12 -> if (Hex.debug) infiniteMoney = !infiniteMoney
      Keys.Z -> if (Keys.CONTROL_LEFT.isKeyPressed()) screen.island.history.undo()
      Keys.Y -> if (Keys.CONTROL_LEFT.isKeyPressed()) screen.island.history.redo()

      else -> {
        if (screen.island.inHand == null || screen.island.inHand?.piece?.data === HexagonData.EDGE_DATA) {
          val piece = keycodeToPiece(keycode) ?: return false
          return buyUnit(piece)
        }
      }
    }
    return true
  }

  fun buyUnit(piece: Piece): Boolean {
    screen.island.selected?.also { territory ->
      if (!infiniteMoney) {
        if (!territory.capital.canBuy(piece)) {
          return@also
        }
        territory.capital.balance -= piece.price
      }

      screen.island.history.remember("Buying piece") {
        if (piece is LivingPiece) piece.moved = false
        screen.island.inHand = Hand(territory, piece)
      }
    }
    return true
  }

  companion object {
    fun keycodeToPiece(keycode: Int): Piece? {
      return when (keycode) {
        Keys.F1 -> Castle(HexagonData.EDGE_DATA)
        Keys.F2 -> Peasant(HexagonData.EDGE_DATA)
        Keys.F3 -> Spearman(HexagonData.EDGE_DATA)
        Keys.F4 -> Knight(HexagonData.EDGE_DATA)
        Keys.F5 -> Baron(HexagonData.EDGE_DATA)
        else -> return null
      }
    }
  }
}
