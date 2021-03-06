package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Input.Keys.BACKSPACE
import com.badlogic.gdx.Input.Keys.F11
import com.badlogic.gdx.Input.Keys.F12
import com.badlogic.gdx.Input.Keys.SPACE
import com.badlogic.gdx.Input.Keys.Y
import com.badlogic.gdx.Input.Keys.Z
import no.elg.hex.Hex
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.HexagonData.Companion.EDGE_DATA
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
import no.elg.hex.util.calculateRing
import no.elg.hex.util.canAttack
import no.elg.hex.util.createInstance
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.isKeyPressed
import no.elg.hex.util.toggleShown
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.isSubclassOf

/** @author Elg */
class GameInputProcessor(val screen: PlayableIslandScreen) : AbstractInput(true) {

  var infiniteMoney = Hex.args.cheating

  private fun pickUp(island: Island, hexData: HexagonData, territory: Territory): Boolean {
    val cursorPiece = hexData.piece
    if (cursorPiece.movable &&
      cursorPiece is LivingPiece &&
      !cursorPiece.moved &&
      hexData.team == island.currentTeam
    ) {
      // We currently don't hold anything in our hand, so pick it up!
      island.history.remember("Pickup piece") {
        island.hand = Hand(territory, cursorPiece)
        hexData.setPiece(Empty::class)
      }
      Gdx.app.trace("PLACE", "Hand was null, now it is ${island.hand}")
      return true
    }
    return false
  }

  private fun placeDown(
    island: Island,
    territory: Territory,
    placeOn: Hexagon<HexagonData>,
    newPiece: Piece,
  ): Boolean {
    val hexData = island.getData(placeOn)
    if (hexData.team != island.currentTeam && !territory.enemyBorderHexes.contains(placeOn)) {
      Gdx.app.debug("PLACE", "Tried to place piece on enemy hex outside border hexes")
      return false
    }

    val oldPiece = hexData.piece

    val (newPieceType, moved) = if (oldPiece is LivingPiece &&
      newPiece is LivingPiece &&
      hexData.team == territory.team
    ) {
      // merge cursor piece with held piece
      if (newPiece.canNotMerge(oldPiece)) return true
      // The piece can only move when both the piece in hand and the hex pointed at has not moved
      strengthToType(newPiece.strength + oldPiece.strength) to (newPiece.moved || oldPiece.moved)
    } else {
      newPiece::class to (hexData.team != island.currentTeam || oldPiece !is Empty)
    }

    if (newPieceType.isSubclassOf(LivingPiece::class)) {
      if (hexData.team == territory.team && (oldPiece is Capital || oldPiece is Castle)) {
        Gdx.app.debug("PLACE", "Cannot place a living entity of the same team onto a capital or castle piece")
        return true
      } else if (hexData.team != territory.team && !island.canAttack(placeOn, newPiece)) {
        Gdx.app.debug("PLACE", "Cannot place castle on an enemy hex")
        return true
      }
    } else if (Castle::class == newPieceType) {
      if (hexData.team != territory.team) {
        Gdx.app.debug("PLACE", "Cannot attack ${oldPiece::class.simpleName} with a ${newPiece::class.simpleName}")
        return true
      }
    } else if (Castle::class != newPieceType) {
      throw IllegalStateException("Holding illegal piece '$newPieceType', can only hold living pieces and castle!")
    }

    if (hexData.setPiece(newPieceType)) {
      island.history.remember("Placing piece") {
        hexData.team = territory.team

        island.hand?.currentHand = false

        val updatedPiece = hexData.piece
        if (updatedPiece is LivingPiece) {
          updatedPiece.moved = moved
        }

        for (neighbor in island.getNeighbors(placeOn)) {
          island.findTerritory(neighbor)
        }

        // reselect territory to update its values
        island.select(placeOn)
        screen.checkEndedGame()
      }
    }
    return true
  }

  /**
   * @param hexagon The hexagon the player clicked
   * @param longPress If the click was a long press, always false when AI is clicking
   */
  fun click(hexagon: Hexagon<HexagonData>, longPress: Boolean = false): Boolean {
    val island = screen.island
    val cursorHexData = island.getData(hexagon)

    val oldTerritory = island.selected
    if ((oldTerritory == null || !oldTerritory.hexagons.contains(hexagon)) && cursorHexData.team == island.currentTeam) {
      island.select(hexagon)
    }
    val territory = island.selected
    if (territory == null) {
      Gdx.app.debug("PLACE", "Territory is still null after selecting it")
      return false
    }

    val hand = island.hand
    return when {
      longPress -> march(hexagon)
      hand == null -> pickUp(island, cursorHexData, territory)
      else -> placeDown(island, territory, hexagon, hand.piece)
    }
  }

  override fun keyDown(keycode: Int): Boolean {
    if (screen.island.isCurrentTeamAI()) return false

    when (keycode) {
      BACKSPACE, SPACE -> {
        if (screen.island.hand != null) {
          screen.island.hand = null
        } else {
          screen.island.select(null)
        }
      }
      F12 -> if (Hex.debug) infiniteMoney = !infiniteMoney
      F11 -> if (Hex.debug) screen.acceptAISurrender.toggleShown(screen.stage)
      Z -> if (Keys.CONTROL_LEFT.isKeyPressed()) screen.island.history.undo()
      Y -> if (Keys.CONTROL_LEFT.isKeyPressed()) screen.island.history.redo()

      else -> {
        if (screen.island.hand == null || screen.island.hand?.piece?.data === EDGE_DATA) {
          val piece = keycodeToPiece(keycode) ?: return false
          return buyUnit(piece)
        }
      }
    }
    return true
  }

  fun buyUnit(piece: Piece): Boolean {
    screen.island.selected?.also { territory ->

      val hand = screen.island.hand
      if (hand != null && (
        piece !is LivingPiece && hand.piece !is LivingPiece && piece::class == hand.piece::class ||
          piece is LivingPiece && hand.piece is LivingPiece && piece.canNotMerge(hand.piece)
        )
      ) {
        // If we cannot merge or the pieces are identical we should not be able to buy new pieces
        return@also
      }

      if (!infiniteMoney) {
        if (!territory.capital.canBuy(piece)) {
          return@also
        }
        territory.capital.balance -= piece.price
      }

      if (piece is LivingPiece) piece.moved = false
      if (hand != null &&
        piece is LivingPiece &&
        hand.piece is LivingPiece
      ) {
        require(piece.canMerge(hand.piece))

        screen.island.history.remember("Buying piece") {
          val newType = strengthToType(hand.piece.strength + piece.strength)
          Gdx.app.trace("BUY") { "Bought ${piece::class.simpleName} while holding ${hand.piece::class.simpleName}" }

          val data = hand.piece.data

          hand.refund = false
          screen.island.hand = null

          val newPiece = newType.createInstance(data)
          newPiece.moved = false

          screen.island.hand = Hand(territory, newPiece)
        }
      } else {
        screen.island.history.remember("Buying piece") {
          screen.island.hand = Hand(territory, piece)
        }
      }
    }
    return true
  }

  private fun march(hexagon: Hexagon<HexagonData>): Boolean {
    screen.island.selected?.also { territory ->
      if (hexagon !in territory.hexagons) return false
      val pieces = mutableListOf<LivingPiece>()
      territory.hexagons.map { screen.island.getData(it) }.filter {
        val piece = it.piece
        piece is LivingPiece && !piece.moved
      }.forEach {
        pieces += it.piece as LivingPiece
        it.setPiece(Empty::class)
      }
      Gdx.app.trace("MARCH", "Marching ${pieces.size} pieces")
      if (pieces.isEmpty()) return false

      pieces.sortByDescending { it.strength }

      val iter = pieces.iterator()

      val cursorData = screen.island.getData(hexagon)

      screen.island.history.remember("March") {
        if (cursorData.piece is Empty) {
          val piece = iter.next()
          cursorData.setPiece(piece::class) {
            moved = false
          }
          require(cursorData.piece is LivingPiece) { "New piece is not Living Piece" }
        }

        var radius = 1
        while (iter.hasNext()) {
          for (hex in screen.island.calculateRing(hexagon, radius++)) {
            if (hex !in territory.hexagons) continue
            if (!iter.hasNext()) {
              return@remember
            }
            val data = screen.island.getData(hex)
            if (data.piece is Empty) {
              val piece = iter.next()
              val placed = data.setPiece(piece::class) {
                moved = false
              }
              require(placed) { "Failed to place on an empty hexagon" }
            }
          }
        }
      }
      return true
    }
    return false
  }

  private fun click(longPress: Boolean): Boolean {
    if (screen.island.isCurrentTeamAI()) return false
    val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false
    return click(cursorHex, longPress)
  }

  override fun longPress(x: Float, y: Float): Boolean = click(true)
  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean = click(false)

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
