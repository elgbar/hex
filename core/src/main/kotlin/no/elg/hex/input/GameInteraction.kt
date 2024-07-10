package no.elg.hex.input

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.strengthToType
import no.elg.hex.island.Hand
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.util.calculateRing
import no.elg.hex.util.canAttack
import no.elg.hex.util.createInstance
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.isPartOfATerritory
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.isSubclassOf

/**
 * Handle high-level interaction with the game.
 * This is used both by the player and by AIs to ensure everyone interacts with the game in a fair manner.
 */
class GameInteraction(val island: Island, val endGame: (won: Boolean) -> Unit) {

  internal var cheating = Hex.args.cheating

  fun endGame() = endGame(island.isCurrentTeamHuman())

  /**
   * @param hexagon The hexagon the player clicked
   * @param longPress If the click was a long press, always false when AI is clicking
   */
  fun click(hexagon: Hexagon<HexagonData>, longPress: Boolean = false): Boolean {
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

  fun buyUnit(piece: Piece): Boolean {
    island.selected?.also { territory ->
      val hand = island.hand
      if (hand != null && (
          piece !is LivingPiece && hand.piece !is LivingPiece && piece::class == hand.piece::class ||
            piece is LivingPiece && hand.piece is LivingPiece && piece.canNotMerge(hand.piece)
          )
      ) {
        // If we cannot merge or the pieces are identical we should not be able to buy new pieces
        return false
      }

      if (!cheating || island.isCurrentTeamAI()) {
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

        island.history.remember("Upgrading held piece") {
          val newType = strengthToType(hand.piece.strength + piece.strength)
          Gdx.app.trace("BUY") { "Bought ${piece::class.simpleName} while holding ${hand.piece::class.simpleName}" }

          val data = hand.piece.data

          hand.restore = Hand.Companion.NoRestore
          island.hand = null

          val newPiece = newType.createInstance(data)
          newPiece.moved = false

          island.hand = Hand(territory, newPiece)
        }
      } else {
        island.history.remember("Buying piece") {
          island.hand = Hand(territory, piece)
        }
      }
      Hex.assets.pieceDownSound?.play(Settings.volume)
      return true
    }
    return false
  }

  private fun pickUp(island: Island, hexData: HexagonData, territory: Territory): Boolean {
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
        pieceChanged = hexData.setPiece<Empty>()
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
    val hexData = island.getData(placeOn)
    val isBorderHexagon = territory.enemyBorderHexes.contains(placeOn)
    if (hexData.team != island.currentTeam && !isBorderHexagon) {
      Gdx.app.debug("PLACE", "Tried to place piece on enemy hex outside border hexes")
      return false
    }

    val oldPiece = hexData.piece
    if (oldPiece is LivingPiece && newPiece is Castle && hexData.team == territory.team) {
      if (!oldPiece.moved) {
        island.history.remember("Swapping castle for living piece") {
          hexData.setPiece<Castle>() {
            island.hand?.restore = Hand.Companion.NoRestore
            island.hand = Hand(territory, oldPiece, restore = Hand.Companion.RefundCastleSwapAction)
          }
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
        island.hand?.restore = Hand.Companion.NoRestore
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
        if (island.checkGameEnded()) {
          endGame()
        }
      }
    }
    return true
  }

  private fun march(to: Hexagon<HexagonData>): Boolean {
    if (!Settings.enableHoldToMarch) return false

    island.selected?.also { territory ->
      if (to !in territory.hexagons) return false
      val pieces = mutableListOf<LivingPiece>()
      territory.hexagons.map { island.getData(it) }.filter {
        val piece = it.piece
        piece is LivingPiece && !piece.moved
      }.forEach {
        pieces += it.piece as LivingPiece
        it.setPiece<Empty>()
      }
      Gdx.app.trace("MARCH") { "Marching ${pieces.size} pieces" }
      if (pieces.isEmpty()) return false

      pieces.sortByDescending { it.strength }

      val iter = pieces.iterator()

      val cursorData = island.getData(to)

      island.history.remember("March") {
        if (cursorData.piece is Empty) {
          val piece = iter.next()
          cursorData.setPiece(piece::class) {
            it.moved = false
          }
          require(cursorData.piece is LivingPiece) { "New piece is not Living Piece" }
        }

        var radius = 1
        while (iter.hasNext()) {
          for (hex in island.calculateRing(to, radius++)) {
            if (hex !in territory.hexagons) continue
            if (!iter.hasNext()) {
              return@remember
            }
            val data = island.getData(hex)
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
}