package no.elg.hex.island

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Piece

/**
 *
 * TODO refactor into a sealed class. One hand for picked up pieces and one for bought pieces
 * @author Elg
 */
data class Hand(val territory: Territory, val piece: Piece) {

  init {
    require(piece !is LivingPiece || !piece.moved) { "Holding a piece that has already moved!" }
  }

  fun undoPickup() {
    if (piece.data !== HexagonData.EDGE_DATA) {
      val newPiece = piece.data.piece
      if (piece.data.setPiece(piece::class) && newPiece is LivingPiece) {
        newPiece.moved = false
      }
    } else {
      territory.capital.balance += piece.price
    }
  }

  override fun toString(): String {
    return "team: $territory, piece: ${piece::class.simpleName}"
  }
}

