package no.elg.hex.input

import com.badlogic.gdx.Gdx
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
import no.elg.hex.Settings
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
import no.elg.hex.util.isPartOfATerritory
import no.elg.hex.util.toggleShown
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.isSubclassOf

/** @author Elg */
class GameInputProcessor(val screen: PlayableIslandScreen) : AbstractInput(true) {

  var cheating = Hex.args.cheating
    private set

  private fun pickUp(island: Island, hexData: HexagonData, territory: Territory): Boolean {
    if (screen.isDisposed) {
      return false
    }
    val cursorPiece = hexData.piece
    if (cursorPiece.movable &&
      cursorPiece is LivingPiece &&
      !cursorPiece.moved &&
      hexData.team == island.currentTeam
    ) {
      var pieceChanged = false
      // We currently don't hold anything in our hand, so pick it up!
      island.history.remember("Pickup piece") {
        island.hand = Hand(territory, cursorPiece)
        pieceChanged = hexData.setPiece(Empty::class)
      }
      Gdx.app.trace("PLACE") { "Hand was null, now it is ${island.hand}" }
      return pieceChanged
    }
    return false
  }

  private fun placeDown(
    island: Island,
    territory: Territory,
    placeOn: Hexagon<HexagonData>,
    newPiece: Piece
  ): Boolean {
    if (screen.isDisposed) {
      return false
    }
    val hexData = island.getData(placeOn)
    val isBorderHexagon = territory.enemyBorderHexes.contains(placeOn)
    if (hexData.team != island.currentTeam && !isBorderHexagon) {
      Gdx.app.debug("PLACE", "Tried to place piece on enemy hex outside border hexes")
      return false
    }

    val oldPiece = hexData.piece
    if (oldPiece is LivingPiece && newPiece is Castle && hexData.team == territory.team) {
      if (!oldPiece.moved && hexData.setPiece(Castle::class)) {
        island.history.remember("Swapping castle for living piece") {
          island.hand = Hand(territory, oldPiece, restore = false)
        }
        return true
      }
      return false
    }

    val (newPieceType, moved) = if (oldPiece is LivingPiece && newPiece is LivingPiece && hexData.team == territory.team) {
      // merge cursor piece with held piece
      if (newPiece.canNotMerge(oldPiece)) {
        Gdx.app.debug("PLACE", "Cannot merge ${oldPiece::class.simpleName} with a ${newPiece::class.simpleName}")
        return false
      }
      // The piece can only move when both the piece in hand and the hex pointed at has not moved
      strengthToType(newPiece.strength + oldPiece.strength) to (newPiece.moved || oldPiece.moved)
    } else {
      newPiece::class to (hexData.team != island.currentTeam || oldPiece !is Empty)
    }

    if (newPieceType.isSubclassOf(LivingPiece::class)) {
      if (hexData.team == territory.team && (oldPiece is Capital || oldPiece is Castle)) {
        Gdx.app.debug("PLACE", "Cannot place a living entity of the same team onto a capital or castle piece")
        return false
      } else if (hexData.team != territory.team && !island.canAttack(placeOn, newPiece)) {
        Gdx.app.debug("PLACE", "Cannot place castle on an enemy hex")
        return false
      }
    } else if (Castle::class == newPieceType) {
      if (hexData.team != territory.team) {
        Gdx.app.debug("PLACE", "Cannot attack ${oldPiece::class.simpleName} with a ${newPiece::class.simpleName}")
        return false
      }
    } else {
      throw IllegalStateException("Holding illegal piece '$newPieceType', can only hold living pieces and castle!")
    }

    if (hexData.setPiece(newPieceType)) {
      island.history.remember("Placing piece") {
        hexData.team = territory.team
        // Never refund as we are placing the unit down right now
        island.hand?.restore = false
        island.hand = null

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
    if (screen.isDisposed) {
      return false
    }
    val island = screen.island
    val cursorHexData = island.getData(hexagon)

    val oldTerritory = island.selected
    if ((oldTerritory == null || !oldTerritory.hexagons.contains(hexagon)) && cursorHexData.team == island.currentTeam) {
      if (hexagon.isPartOfATerritory(island)) {
        island.select(hexagon)
      } else {
        return false
      }
    }
    val territory = island.selected
    if (territory == null) {
      Gdx.app.debug("PLACE", "Territory is still null after selecting it")
      return false
    }
    val selectedTerritory = territory != oldTerritory && !cursorHexData.piece.movable

    fun playSound(action: Boolean) {
      if (action) {
        Hex.assets.pieceDownSound?.play(Settings.volume)
      } else if (selectedTerritory) {
        val sound = if (territory.income > 0) Hex.assets.coinsSound else Hex.assets.emptyCoffersSound
        sound?.play(Settings.volume)
      }
    }

    val hand = island.hand
    return when {
      longPress -> march(hexagon)
      hand == null -> pickUp(island, cursorHexData, territory).also { playSound(it) }
      else -> placeDown(island, territory, hexagon, hand.piece).also { playSound(it) }
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

      F12 -> if (Hex.debug || Hex.args.cheating) cheating = !cheating
      F11 -> if (cheating) screen.acceptAISurrender.toggleShown(screen.stage)
      F10 -> if (cheating) {
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
          buyUnit(piece)
        }
      }
    }
    return true
  }

  fun buyUnit(piece: Piece): Boolean {
    if (screen.isDisposed) {
      return false
    }
    screen.island.selected?.also { territory ->
      val hand = screen.island.hand
      if (hand != null && (
          piece !is LivingPiece && hand.piece !is LivingPiece && piece::class == hand.piece::class ||
            piece is LivingPiece && hand.piece is LivingPiece && piece.canNotMerge(hand.piece)
          )
      ) {
        // If we cannot merge or the pieces are identical we should not be able to buy new pieces
        return false
      }

      if (!cheating || screen.island.isCurrentTeamAI()) {
        if (!territory.capital.canBuy(piece)) {
          return false
        }
        territory.capital.balance -= piece.price
      }

      if (piece is LivingPiece) piece.moved = false
      if (hand != null &&
        piece is LivingPiece &&
        hand.piece is LivingPiece
      ) {
        require(piece.canMerge(hand.piece))

        screen.island.history.remember("Upgrading held piece") {
          val newType = strengthToType(hand.piece.strength + piece.strength)
          Gdx.app.trace("BUY") { "Bought ${piece::class.simpleName} while holding ${hand.piece::class.simpleName}" }

          val data = hand.piece.data

          hand.restore = false
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
      Hex.assets.pieceDownSound?.play(Settings.volume)
      return true
    }
    return false
  }

  private fun march(hexagon: Hexagon<HexagonData>): Boolean {
    if (screen.isDisposed) {
      return false
    }
    if (!Settings.enableHoldToMarch) return false

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
      Gdx.app.trace("MARCH") { "Marching ${pieces.size} pieces" }
      if (pieces.isEmpty()) return false

      pieces.sortByDescending { it.strength }

      val iter = pieces.iterator()

      val cursorData = screen.island.getData(hexagon)

      screen.island.history.remember("March") {
        if (cursorData.piece is Empty) {
          val piece = iter.next()
          cursorData.setPiece(piece::class) {
            it.moved = false
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
                it.moved = false
              }
              require(placed) { "Failed to place on an empty hexagon" }
            }
          }
        }
      }
      Hex.assets.pieceDownSound?.play(Settings.volume)
      return true
    }
    return false
  }

  private fun click(longPress: Boolean): Boolean {
    if (screen.isDisposed) {
      return false
    }
    if (screen.island.isCurrentTeamAI()) return false
    val cursorHex = screen.basicIslandInputProcessor.cursorHex ?: return false
    return click(cursorHex, longPress)
  }

  override fun longPress(x: Float, y: Float): Boolean = click(true)
  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean =
    if (count <= 1) {
      click(false)
    } else {
      super.tap(x, y, count, button)
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