package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.input.MapEditorInputHandler.selectedTeam
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

sealed class TeamEditor : Editor() {

  companion object {
    val TEAM_EDITORS: List<TeamEditor> by lazy {
      TeamEditor::class.sealedSubclasses.map {
        requireNotNull(it.objectInstance) { "All subclasses of ${TeamEditor::class::simpleName} must be objects" }
      }.also {
        val disabledSubclasses = it.filter { sub -> sub.isNOP }.size
        require(disabledSubclasses == 1) {
          "There must be one and exactly one disabled subclass of ${TeamEditor::class::simpleName}. Found $disabledSubclasses disabled classes."
        }
      }
    }
  }

  object `Set team` : TeamEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().setTeam(selectedTeam, null)
    }
  }

  object `Randomize team` : TeamEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().setTeam(Team.values().random(), null)
    }
  }

  object Disabled : TeamEditor() {
    override val isNOP = true
  }
}
