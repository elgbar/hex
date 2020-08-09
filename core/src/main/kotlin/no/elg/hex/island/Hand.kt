package no.elg.hex.island

import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Piece

/** @author Elg */
data class Hand(val territory: Territory, val piece: Piece) {

  init {
    require(piece !is LivingPiece || !piece.moved) { "Holding a piece that has already moved!" }
  }

  var holding = true

  override fun toString(): String {
    return "team: $territory, piece: ${piece::class.simpleName}"
  }
}
