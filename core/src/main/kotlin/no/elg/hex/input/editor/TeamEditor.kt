package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import org.hexworks.mixite.core.api.Hexagon

sealed interface TeamEditor : Editor {

  object SetTeam : TeamEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.team = metadata.selectedTeam
    }
  }

  object RandomizeTeam : TeamEditor {

    override val order: Int = 0

    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.team = Team.entries.toTypedArray().random()
    }
  }
}