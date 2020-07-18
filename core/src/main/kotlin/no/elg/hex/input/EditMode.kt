package no.elg.hex.input

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

sealed class EditMode {

  companion object {
    val editModeSubclasses: List<EditMode> by lazy {
      EditMode::class.sealedSubclasses.map {
        requireNotNull(it.objectInstance) { "All subclasses of ${EditMode::class::simpleName} must be objects" }
      }
    }
  }

  val name: String = requireNotNull(this::class.simpleName) { "Subclass of ${EditMode::class::simpleName} cannot be anonymous" }

  abstract fun edit(hexagon: Hexagon<HexagonData>)

  object Add : EditMode() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isOpaque = false
    }
  }

  object Delete : EditMode() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isOpaque = true
    }
  }

  object Or : EditMode() {
    override fun edit(hexagon: Hexagon<HexagonData>) {
      hexagon.getData().isOpaque = !hexagon.getData().isOpaque
    }
  }
}
