package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.input.MapEditorInputProcessor
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.primaryConstructor

sealed class TeamEditor(val islandScreen: IslandScreen) : Editor() {

  companion object {

    fun generateTeamEditors(islandScreen: IslandScreen): List<TeamEditor> =
      TeamEditor::class.sealedSubclasses.map {
        it.primaryConstructor?.call(islandScreen) ?: error("Failed to create new instance of ${it.simpleName}")
      }.also {
        val disabledSubclasses = it.filter { sub -> sub.isNOP }.size
        require(disabledSubclasses == 1) {
          "There must be one and exactly one disabled subclass of ${TeamEditor::class::simpleName}. Found $disabledSubclasses disabled classes."
        }
      }

  }

  class `Set team`(islandScreen: IslandScreen) : TeamEditor(islandScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      require(islandScreen.inputProcessor is MapEditorInputProcessor) {
        "Tried change editor while the input processor is not ${MapEditorInputProcessor::class.simpleName}"
      }
      hexagon.getData(islandScreen.island).team = (islandScreen.inputProcessor as MapEditorInputProcessor).selectedTeam
    }
  }

  class `Randomize team`(islandScreen: IslandScreen) : TeamEditor(islandScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData(islandScreen.island).team = Team.values().random()
    }
  }

  class Disabled(islandScreen: IslandScreen) : TeamEditor(islandScreen) {
    override val isNOP = true
  }
}
