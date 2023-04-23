package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.primaryConstructor

sealed class OpaquenessEditor(val mapEditorScreen: MapEditorScreen) : Editor {

  companion object {

    fun generateOpaquenessEditors(mapEditorScreen: MapEditorScreen): List<OpaquenessEditor> =
      OpaquenessEditor::class.sealedSubclasses.map {
        it.primaryConstructor?.call(mapEditorScreen)
          ?: error("Failed to create new instance of ${it.simpleName}")
      }
  }

  class SetOpaque(mapEditorScreen: MapEditorScreen) : OpaquenessEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      mapEditorScreen.island.getData(hexagon).isDisabled = false
    }
  }

  class SetTransparent(mapEditorScreen: MapEditorScreen) : OpaquenessEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      mapEditorScreen.island.getData(hexagon).isDisabled = true
    }
  }

  class ToggleOpaqueness(mapEditorScreen: MapEditorScreen) : OpaquenessEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      val data = mapEditorScreen.island.getData(hexagon)
      data.isDisabled = !data.isDisabled
    }
  }
}