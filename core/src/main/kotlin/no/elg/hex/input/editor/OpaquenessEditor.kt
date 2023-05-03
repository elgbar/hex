@file:Suppress("unused")

package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import org.hexworks.mixite.core.api.Hexagon

sealed interface OpaquenessEditor : Editor {

  override fun postEdit(metadata: EditMetadata) {
//    metadata.island.recalculateVisibleIslands()
  }

  object SetOpaque : OpaquenessEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.isDisabled = false
    }
  }

  object SetTransparent : OpaquenessEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.isDisabled = true
    }
  }

  object ToggleOpaqueness : OpaquenessEditor {

    override val order: Int = 0

    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      val shouldDisable = metadata.clickedHexagonData.invisible
      data.isDisabled = !shouldDisable
    }
  }
}