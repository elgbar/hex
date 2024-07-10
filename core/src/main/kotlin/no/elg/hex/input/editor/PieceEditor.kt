package no.elg.hex.input.editor

import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.PIECES
import no.elg.hex.hexagon.Piece
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.KClass

sealed interface PieceEditor : Editor {

  companion object {
    fun HexagonData.trySetPiece(pieceClass: KClass<out Piece>) {
      if (visible || pieceClass == Empty::class) {
        setPiece(pieceClass)
      }
    }
  }

  data object SetPiece : PieceEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.trySetPiece(metadata.selectedPiece)
    }
  }

  data object RandomizePiece : PieceEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.trySetPiece(PIECES.random())
    }
  }
}