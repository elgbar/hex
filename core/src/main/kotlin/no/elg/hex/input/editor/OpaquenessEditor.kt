package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.full.primaryConstructor

sealed class OpaquenessEditor(val islandScreen: IslandScreen) : Editor() {

  companion object {

    fun generateOpaquenessEditors(islandScreen: IslandScreen): List<OpaquenessEditor> =
      OpaquenessEditor::class.sealedSubclasses.map {
        it.primaryConstructor?.call(islandScreen) ?: error("Failed to create new instance of ${it.simpleName}")
      }.also {
        val disabledSubclasses = it.filter { sub -> sub.isNOP }.size
        require(disabledSubclasses == 1) {
          "There must be one and exactly one disabled subclass of ${OpaquenessEditor::class::simpleName}. Found $disabledSubclasses disabled classes."
        }
      }
  }

  class `Set opaque`(islandScreen: IslandScreen) : OpaquenessEditor(islandScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData(islandScreen.island).isOpaque = false
    }
  }

  class `Set transparent`(islandScreen: IslandScreen) : OpaquenessEditor(islandScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData(islandScreen.island).isOpaque = true
    }
  }

  class `Toggle opaqueness`(islandScreen: IslandScreen) : OpaquenessEditor(islandScreen) {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      val data = hexagon.getData(islandScreen.island)
      data.isOpaque = !data.isOpaque
    }
  }

  class Disabled(islandScreen: IslandScreen) : OpaquenessEditor(islandScreen) {
    override val isNOP = true
  }
}
