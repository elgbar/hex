package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

sealed class OpaquenessEditor : Editor() {

  companion object {
    val OPAQUENESS_EDITORS: List<OpaquenessEditor> by lazy {
      OpaquenessEditor::class.sealedSubclasses.map {
        requireNotNull(it.objectInstance) { "All subclasses of ${OpaquenessEditor::class::simpleName} must be objects" }
      }.also {
        val disabledSubclasses = it.filter { sub -> sub.isNOP }.size
        require(disabledSubclasses == 1) {
          "There must be one and exactly one disabled subclass of ${OpaquenessEditor::class::simpleName}. Found $disabledSubclasses disabled classes."
        }
      }
    }
  }

  object `Set opaque` : OpaquenessEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isOpaque = false
    }
  }

  object `Set transparent` : OpaquenessEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isOpaque = true
    }
  }

  object `Toggle opaqueness` : OpaquenessEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isOpaque = !hexagon.getData().isOpaque
    }
  }

  object Disabled : OpaquenessEditor() {
    override val isNOP = true
  }
}
