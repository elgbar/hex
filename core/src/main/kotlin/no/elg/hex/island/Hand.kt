package no.elg.hex.island

import com.badlogic.gdx.utils.Disposable
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Piece

/** @author Elg */
data class Hand(
  val territory: Territory,
  val piece: Piece
) : Disposable {

  init {
    require(piece !is LivingPiece || !piece.moved) { "Holding a piece that has already moved" }
    require(piece != Empty) { "Cannot hold empty piece" }
  }

  var currentHand = true

  override fun dispose() {
    require(currentHand) { "Hand already disposed " }
    currentHand = false
    if (piece.data === HexagonData.EDGE_DATA) {
      // refund when placing it back
      territory.capital.balance += piece.price
    } else {
      val placed = piece.data.setPiece(piece::class)
      require(placed) { "Failed to place old hand piece back to where it was" }
      // restore the piece to its last placed location
      val newPiece = piece.data.piece
      if (newPiece is LivingPiece) {
        newPiece.moved = false
      }
    }
  }

  override fun toString(): String {
    return "team: $territory, piece: ${piece::class.simpleName}"
  }
}
