package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.PIECES
import no.elg.hex.input.MapEditorInputProcessor
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.primaryConstructor

sealed class PieceEditor(val islandScreen: IslandScreen) : Editor() {

  companion object {

    fun generatePieceEditors(islandScreen: IslandScreen): List<PieceEditor> =
      PieceEditor::class.sealedSubclasses.map {
        it.primaryConstructor?.call(islandScreen) ?: error("Failed to create new instance of ${it.simpleName}")
      }.also {
        val disabledSubclasses = it.filter { sub -> sub.isNOP }.size
        require(disabledSubclasses == 1) {
          "There must be one and exactly one disabled subclass of ${PieceEditor::class::simpleName}. Found $disabledSubclasses disabled classes."
        }
      }
  }

  class `Set piece`(islandScreen: IslandScreen) : PieceEditor(islandScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      require(islandScreen.inputProcessor is MapEditorInputProcessor) {
        "Tried change editor while the input processor is not ${MapEditorInputProcessor::class.simpleName}"
      }
      hexagon.getData(islandScreen.island).setPiece((islandScreen.inputProcessor as MapEditorInputProcessor).selectedPiece)
    }
  }

  class `Randomize piece`(islandScreen: IslandScreen) : PieceEditor(islandScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData(islandScreen.island).setPiece(PIECES.random())
    }
  }

  class Disabled(islandScreen: IslandScreen) : PieceEditor(islandScreen) {
    override val isNOP = true
  }
}
