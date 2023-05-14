package no.elg.hex.model

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.island.Island
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.HexagonalGridLayout
import java.util.SortedMap

data class IslandDto(
  val width: Int,
  val height: Int,
  val layout: HexagonalGridLayout,
  val territoryCoordinate: CubeCoordinate? = null,
  val handCoordinate: CubeCoordinate? = null,
  val handPiece: Piece? = null,
  val handRestore: Boolean? = null,
  val hexagonData: SortedMap<CubeCoordinate, HexagonData>,
  val round: Int,
  val team: Team = Team.LEAF,
  val authorRoundsToBeat: Int = Island.UNKNOWN_ROUNDS_TO_BEAT
) {
  init {
    require(hexagonData.values.none { it.invisible }) { "IslandDto was given invisible hexagon, all serialized hexagons must be visible" }
  }

  fun copy(): IslandDto {
    return IslandDto(
      width = width,
      height = height,
      layout = layout,
      territoryCoordinate = territoryCoordinate,
      handCoordinate = handCoordinate,
      handPiece = handPiece?.createDtoCopy(),
      hexagonData = hexagonData.mapValues { (_, data) -> data.copy() }.toSortedMap(),
      round = round,
      team = team,
      authorRoundsToBeat = authorRoundsToBeat
    )
  }

  companion object {
    fun Piece?.createDtoCopy(): Piece? {
      return this?.let { it.copyTo(it.data.copy()) }
    }
  }
}