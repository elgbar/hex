package no.elg.hex.input.editor

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.island.Island
import no.elg.hex.util.toTitleCase
import org.hexworks.mixite.core.api.Hexagon
import kotlin.reflect.KClass

/** @author Elg */
sealed interface Editor {
  val name: String
    get() =
      requireNotNull(this::class.simpleName?.toTitleCase()) {
        "Subclass of ${Editor::class::simpleName} cannot be anonymous"
      }

  val order: Int
    get() = Int.MAX_VALUE

  val isNOP
    get() = false

  fun edit(hexagon: Hexagon<HexagonData>, data: HexagonData, metadata: EditMetadata)

  fun postEdit(metadata: EditMetadata) = Unit
}

val editorsList: List<List<Editor>> by lazy {
  Editor::class.sealedSubclasses.map { subclass ->
    if (subclass.isSealed) {
      subclass.sealedSubclasses.map { it.objectInstance ?: error("Failed to create new instance of ${it.simpleName}") }.sortedBy { it.order }
    } else {
      listOf(subclass.objectInstance ?: error("Failed to create new instance of ${subclass.simpleName}"))
    }
  }
}

data class EditMetadata(
  /**
   * The hexagon clicked on
   */
  val clickedHexagon: Hexagon<HexagonData>,
  /**
   * A copy of the original data before any editing taking place
   */
  val clickedHexagonData: HexagonData,
  /**
   * The island we are editing on
   */
  val island: Island,
  val selectedPiece: KClass<out Piece>,
  val selectedTeam: Team
)