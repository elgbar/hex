package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.PIECES
import org.hexworks.mixite.core.api.Hexagon

sealed interface PieceEditor : Editor {

  object SetPiece : PieceEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.setPiece(metadata.selectedPiece)
    }
  }

  object RandomizePiece : PieceEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.setPiece(PIECES.random())
    }
  }
}