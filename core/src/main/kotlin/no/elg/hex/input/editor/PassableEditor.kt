package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

sealed class PassableEditor : Editor() {

  companion object {
    val PASSABLE_EDITORS: List<PassableEditor> by lazy {
      PassableEditor::class.sealedSubclasses.map {
        requireNotNull(it.objectInstance) { "All subclasses of ${PassableEditor::class::simpleName} must be objects" }
      }.also {
        val disabledSubclasses = it.filter { sub -> sub.isNOP }.size
        require(disabledSubclasses == 1) {
          "There must be one and exactly one disabled subclass of ${PassableEditor::class::simpleName}. Found $disabledSubclasses disabled classes."
        }
      }
    }
  }

  object `Set inpassable` : PassableEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isPassable = false
    }
  }

  object `Set passable` : PassableEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isPassable = true
    }
  }

  object `Toggle passable` : PassableEditor() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isPassable = !hexagon.getData().isPassable
    }
  }

  object Disabled : PassableEditor() {
    override val isNOP = true
  }
}
