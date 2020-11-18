package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.PIECES
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.primaryConstructor

sealed class PieceEditor(val mapEditorScreen: MapEditorScreen) : Editor {

  companion object {

    fun generatePieceEditors(mapEditorScreen: MapEditorScreen): List<PieceEditor> =
      PieceEditor::class.sealedSubclasses.map {
        it.primaryConstructor?.call(mapEditorScreen)
          ?: error("Failed to create new instance of ${it.simpleName}")
      }
  }

  class `Set piece`(mapEditorScreen: MapEditorScreen) : PieceEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      mapEditorScreen.island.getData(hexagon).setPiece(mapEditorScreen.selectedPiece)
    }
  }

  class `Randomize piece`(mapEditorScreen: MapEditorScreen) : PieceEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      mapEditorScreen.island.getData(hexagon).setPiece(PIECES.random())
    }
  }
}
