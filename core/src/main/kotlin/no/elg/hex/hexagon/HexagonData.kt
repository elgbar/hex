package no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import no.elg.island.Island
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.defaults.DefaultSatelliteData
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

@JsonInclude(NON_DEFAULT)
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class)
data class HexagonData(
  /**
   * Edge hexagons are hexagons along the edge of the grid. Due to how hexagon detection works these hexagon would be
   * returned even when the mouse is not inside the hexagon In order to prevent that and gain pixel perfect hexagon
   * accuracy the player should not know these exists.
   *
   * @see no.elg.hex.util.getHexagon
   * @see no.elg.hex.hexagon.renderer.OutlineRenderer
   * @see no.elg.hex.hexagon.renderer.VerticesRenderer
   */
  val edge: Boolean = false,

  override var isOpaque: Boolean = edge,

  override var isPassable: Boolean = !edge
) : DefaultSatelliteData() {

  @JsonInclude(ALWAYS)
  var team: Team = Team.values().random()
    private set

  @JsonIgnore
  var piece: Piece = NoPiece
    private set

  /**
   * @param pieceType The new type of piece to place onto this hex, if `null` the piece will not change type
   */
  fun setTeam(team: Team, pieceType: KClass<out Piece>?) {
    require(pieceType?.isAbstract != true) { "Cannot set the piece to an abstract piece" }
    this.team = team
    if (pieceType == null) {
      setPiece(piece::class)
    } else {
      setPiece(pieceType)
    }
  }

  @JsonSetter("pieceType")
  fun setPiece(pieceType: KClass<out Piece>) {
    require(!pieceType.isAbstract) { "Cannot set the piece to an abstract piece" }

    val pieceToPlace = if (pieceType.objectInstance != null) {
      pieceType.objectInstance
    } else {
      pieceType.primaryConstructor?.call(team)
    } ?: error("No constructor found with a single ${Team::class.simpleName} argument")

    if (pieceToPlace.place(this)) {
      piece = pieceToPlace
    }
  }

  @get:JsonIgnore
  val color: Color
    get() = team.color

  @get:JsonIgnore
  val type: HexType
    get() = team.type

  @get:JsonIgnore
  val invisible: Boolean
    get() = edge || isOpaque


  ///////////////////
  // serialization //
  ///////////////////

  @JsonGetter("pieceType")
  fun getPieceTypeName() = piece::class.qualifiedName

  @JsonSetter("pieceType")
  fun setPieceFromTypeName(typeName: String?) {
    setPiece(PIECES.first { it.qualifiedName == typeName })
  }

  override fun toString(): String {
    return "team: $team piece: $piece"
  }

  companion object {

    //Can move to
    const val BRIGHTNESS = 0.9f

    //mouse hovering over, add this to the current hex under the mouse
    const val SELECTED = 0.1f

    private const val EXPECTED_NEIGHBORS = 6

    fun isEdgeHexagon(hex: Hexagon<HexagonData>, island: Island) = island.grid.getNeighborsOf(hex).size != EXPECTED_NEIGHBORS

    val EDGE_DATA = HexagonData(edge = true)
  }
}
