package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.PIECES
import no.elg.hex.input.MapEditorInputHandler.selectedPiece
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

sealed class PieceEditor : Editor() {

  companion object {
    val PIECE_EDITORS: List<PieceEditor> by lazy {
      PieceEditor::class.sealedSubclasses.map {
        requireNotNull(it.objectInstance) { "All subclasses of ${PieceEditor::class::simpleName} must be objects" }
      }.also {
        val disabledSubclasses = it.filter { sub -> sub.isNOP }.size
        require(disabledSubclasses == 1) {
          "There must be one and exactly one disabled subclass of ${PieceEditor::class::simpleName}. Found $disabledSubclasses disabled classes."
        }
      }
    }
  }

  object `Set piece` : PieceEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().setPiece(selectedPiece)
    }
  }

  object `Randomize piece` : PieceEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().setPiece(PIECES.random())
    }
  }

  object Disabled : PieceEditor() {
    override val isNOP = true
  }
}
