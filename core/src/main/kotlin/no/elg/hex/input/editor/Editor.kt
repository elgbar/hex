package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.util.toTitleCase
import org.hexworks.mixite.core.api.Hexagon

/** @author Elg */
interface Editor {
  val name: String
    get() =
      requireNotNull(this::class.simpleName?.toTitleCase()) {
        "Subclass of ${Editor::class::simpleName} cannot be anonymous"
      }

  val isNOP
    get() = false

  fun edit(hexagon: Hexagon<HexagonData>)
}

object NOOPEditor : Editor {
  override val isNOP
    get() = true

  override fun edit(hexagon: Hexagon<HexagonData>) {
    /*NO OP*/
  }
}