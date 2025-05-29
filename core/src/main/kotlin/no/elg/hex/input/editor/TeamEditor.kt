package no.elg.hex.input.editor

import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

sealed interface TeamEditor : Editor {

  data object SetTeam : TeamEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.team = metadata.selectedTeam
    }
  }

  data object RandomizeTeam : TeamEditor {

    override val order: Int = 0

    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.team = Team.entries.random()
    }
  }

  data object RandomizeEveryTeam : TeamEditor {

    override val order: Int = 1

    override fun edit(hexagon: Hexagon<HexagonData>, ignore: HexagonData, metadata: EditMetadata) {
      val allData = metadata.island.allHexagons
        .asSequence()
        .map(metadata.island::getData)

      allData
        .filter { it.piece is Capital }
        .forEach { it.setPiece<Empty>() }

      allData.forEach { it.team = Team.entries.random() }
    }
  }
}