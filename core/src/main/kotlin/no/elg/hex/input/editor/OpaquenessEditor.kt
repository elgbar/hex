package no.elg.hex.input.editor

import kotlin.reflect.full.primaryConstructor
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.screens.MapEditorScreen
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

sealed class OpaquenessEditor(val mapEditorScreen: MapEditorScreen) : Editor {

  companion object {

    fun generateOpaquenessEditors(mapEditorScreen: MapEditorScreen): List<OpaquenessEditor> =
        OpaquenessEditor::class.sealedSubclasses.map {
          it.primaryConstructor?.call(mapEditorScreen)
              ?: error("Failed to create new instance of ${it.simpleName}")
        }
  }

  class `Set opaque`(mapEditorScreen: MapEditorScreen) : OpaquenessEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      mapEditorScreen.island.getData(hexagon).isOpaque = false
    }
  }

  class `Set transparent`(mapEditorScreen: MapEditorScreen) : OpaquenessEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      mapEditorScreen.island.getData(hexagon).isOpaque = true
    }
  }

  class `Toggle opaqueness`(mapEditorScreen: MapEditorScreen) : OpaquenessEditor(mapEditorScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      val data = mapEditorScreen.island.getData(hexagon)
      data.isOpaque = !data.isOpaque
    }
  }
}
