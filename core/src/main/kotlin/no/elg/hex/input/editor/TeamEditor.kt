package no.elg.hex.input.editor

import kotlin.reflect.full.primaryConstructor
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

sealed class TeamEditor(val mapEditorScreen: MapEditorScreen) : Editor {

  companion object {

    fun generateTeamEditors(mapEditorScreen: MapEditorScreen): List<TeamEditor> =
        TeamEditor::class.sealedSubclasses.map {
          it.primaryConstructor?.call(mapEditorScreen)
              ?: error("Failed to create new instance of ${it.simpleName}")
        }
  }

  class `Set team`(mapEditorScreen: MapEditorScreen) : TeamEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {

      mapEditorScreen.island.getData(hexagon).team = mapEditorScreen.inputProcessor.selectedTeam
    }
  }

  class `Randomize team`(mapEditorScreen: MapEditorScreen) : TeamEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      mapEditorScreen.island.getData(hexagon).team = Team.values().random()
    }
  }
}
