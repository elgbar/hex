package no.elg.hex.island

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

  override fun toString(): String {
    return "team: $territory, piece: ${piece::class.simpleName}"
  }
}

