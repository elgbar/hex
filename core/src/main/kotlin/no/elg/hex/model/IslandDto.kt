package no.elg.hex.model

import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.island.Island
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.HexagonalGridLayout
import java.util.SortedMap

class IslandDto(
  val width: Int,
  val height: Int,
  val layout: HexagonalGridLayout,
  val territoryCoordinate: CubeCoordinate? = null,
  val handCoordinate: CubeCoordinate? = null,
  val handPiece: Piece? = null,
  val handRestoreAction: String? = null,
  val hexagonData: SortedMap<CubeCoordinate, HexagonData>,
  val round: Int,
  val team: Team = Team.LEAF
) {
  init {
    require(hexagonData.values.none { it.invisible }) { "IslandDto was given invisible hexagon, all serialized hexagons must be visible" }
  }

  fun refine(island: Island): IslandDto = refine(island.hexagonCoordinateToData)
  fun refine(islandDto: IslandDto): IslandDto = refine(islandDto.hexagonData)

  /**
   * Refine this island dto by only including changes from the given [dataMap].
   *
   * When restoring a refined islandDto the original islandDto must be passed as the base islandDto, so use with care
   */
  fun refine(dataMap: Map<CubeCoordinate, HexagonData>): IslandDto {
    val filter = hexagonData.filter { (coord, _) -> hexagonData[coord] != dataMap[coord] }
    return copy(filter.toSortedMap())
  }

  fun withInitialData(dataMap: Map<CubeCoordinate, HexagonData>): IslandDto = copy(hexagonData = (dataMap + hexagonData).toSortedMap())

  fun copy(hexagonData: SortedMap<CubeCoordinate, HexagonData>? = null): IslandDto =
    IslandDto(
      width = width,
      height = height,
      layout = layout,
      territoryCoordinate = territoryCoordinate,
      handCoordinate = handCoordinate,
      handPiece = handPiece?.createDtoCopy(),
      handRestoreAction = handRestoreAction,
      hexagonData = hexagonData ?: this.hexagonData.mapValues { (_, data) -> data.copy() }.toSortedMap(),
      round = round,
      team = team
    )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as IslandDto

    if (hexagonData.keys != other.hexagonData.keys) return false
    if (!hexagonData.values.zip(other.hexagonData.values).all { (a, b) -> a.equalsWithoutData(b) }) return false

    if (width != other.width) return false
    if (height != other.height) return false
    if (layout != other.layout) return false
    if (territoryCoordinate != other.territoryCoordinate) return false
    if (handCoordinate != other.handCoordinate) return false
    if (handPiece != other.handPiece) return false
    if (handRestoreAction != other.handRestoreAction) return false
    if (round != other.round) return false
    return team == other.team
  }

  override fun hashCode(): Int {
    var result = width
    result = 31 * result + height
    result = 31 * result + layout.hashCode()
    result = 31 * result + (territoryCoordinate?.hashCode() ?: 0)
    result = 31 * result + (handCoordinate?.hashCode() ?: 0)
    result = 31 * result + (handPiece?.hashCode() ?: 0)
    result = 31 * result + (handRestoreAction?.hashCode() ?: 0)
    result = 31 * result + hexagonData.hashCode()
    result = 31 * result + round
    result = 31 * result + team.hashCode()
    return result
  }

  companion object {
    fun Piece?.createDtoCopy(): Piece? = this?.let { it.copyTo(it.data.copy()) }
  }
}