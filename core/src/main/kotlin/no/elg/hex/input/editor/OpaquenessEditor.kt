@file:Suppress("unused")

package no.elg.hex.input.editor

import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hud.MessagesRenderer.publishWarning
import no.elg.hex.util.coordinates
import org.hexworks.mixite.core.api.Hexagon

sealed interface OpaquenessEditor : Editor {

  data object SetOpaque : OpaquenessEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.isDisabled = false
    }
  }

  data object SetTransparent : OpaquenessEditor {
    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      data.isDisabled = true
      if (data.piece !is Empty) {
        publishWarning("Hexagon ${hexagon.coordinates} is had ${data.piece} on it. It has been removed.")
        data.setPiece<Empty>()
      }
    }
  }

  data object ToggleOpaqueness : OpaquenessEditor {

    override val order: Int = 0

    override fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata) {
      if (metadata.clickedHexagonData.invisible) {
        SetOpaque.edit(hexagon, data, metadata)
      } else {
        SetTransparent.edit(hexagon, data, metadata)
      }
    }
  }
}