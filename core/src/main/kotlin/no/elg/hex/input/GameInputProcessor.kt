package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Input.Keys.BACKSPACE
import com.badlogic.gdx.Input.Keys.F11
import com.badlogic.gdx.Input.Keys.F12
import com.badlogic.gdx.Input.Keys.SPACE
import com.badlogic.gdx.Input.Keys.Y
import com.badlogic.gdx.Input.Keys.Z
import com.badlogic.gdx.input.GestureDetector.GestureAdapter
import ktx.app.KtxInputAdapter
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
import no.elg.hex.util.calculateRing
import no.elg.hex.util.canAttack
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.isKeyPressed
import no.elg.hex.util.show
import no.elg.hex.util.toggleShown
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.isSubclassOf

/** @author Elg */
class GameInputProcessor(private val screen: PlayableIslandScreen) : KtxInputAdapter, GestureAdapter() {

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

        val capitals = island.hexagons.map { island.getData(it) }.filter { it.piece is Capital }
        island.currentAI
        if (capitals.count() == 1) {
          screen.updateWinningTurn()
          if (island.isCurrentTeamHuman()) {
            screen.youWon.show(screen.stageScreen.stage)
          } else {
            screen.youLost.show(screen.stageScreen.stage)
          }
        }
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

  override fun keyDown(keycode: Int): Boolean {
    if (screen.island.isCurrentTeamAI()) return false

    when (keycode) {
      BACKSPACE, SPACE -> screen.island.inHand = null
      F12 -> if (Hex.debug) infiniteMoney = !infiniteMoney
      F11 -> if (Hex.debug) screen.acceptAISurrender.toggleShown(screen.stageScreen.stage)
      Z -> if (Keys.CONTROL_LEFT.isKeyPressed()) screen.island.history.undo()
      Y -> if (Keys.CONTROL_LEFT.isKeyPressed()) screen.island.history.redo()

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

  override fun longPress(x: Float, y: Float): Boolean {
    screen.island.selected?.also { territory ->
      val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false
      if (cursorHex !in territory.hexagons) return false
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

      val cursorData = screen.island.getData(cursorHex)
      if (cursorData.piece is Empty) {
        val piece = iter.next()
        cursorData.setPiece(piece::class)
      }

      var radius = 1
      while (iter.hasNext()) {
        for (hex in screen.island.calculateRing(cursorHex, radius++)) {
          if (hex !in territory.hexagons) continue
          if (!iter.hasNext()) {
            return true
          }
          val piece = iter.next()
          val data = screen.island.getData(hex)
          if (data.piece is Empty) {
            val placed = cursorData.setPiece(piece::class)
            require(placed) { "Failed to place on an empty hexagon" }
          }
        }
      }
      return true
    }
    return false
  }

  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
    Gdx.app.log("gest","tap x = [${x}], y = [${y}], count = [${count}], button = [${button}]")
    if (screen.island.isCurrentTeamAI()) return false
    val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false
    click(cursorHex)
    return true
  }

  override fun zoom(initialDistance: Float, distance: Float): Boolean {
    Gdx.app.log("gest","zoom initialDistance = [$initialDistance], distance = [$distance]")
    return screen.basicIslandInputProcessor.scrolled(0f, distance)
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
