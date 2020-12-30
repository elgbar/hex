package no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import no.elg.hex.island.Island
import no.elg.hex.util.createInstance
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.defaults.DefaultSatelliteData
import kotlin.reflect.KClass

@JsonInclude(NON_DEFAULT)
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class)
@JsonIgnoreProperties("id")
data class HexagonData(
  /**
   * Edge hexagons are hexagons along the edge of the grid. Due to how hexagon detection works
   * these hexagon would be returned even when the mouse is not inside the hexagon In order to
   * prevent that and gain pixel perfect hexagon accuracy the player should not know these exists.
   *
   * @see no.elg.hex.util.getHexagon
   * @see no.elg.hex.renderer.OutlineRenderer
   * @see no.elg.hex.renderer.VerticesRenderer
   */
  val edge: Boolean = false,
  override var isOpaque: Boolean = edge,
  override var isPassable: Boolean = !edge
) : DefaultSatelliteData() {

  @JsonInclude(ALWAYS)
  var team: Team = if (edge) Team.SUN else Team.values().random()

  @JsonIgnore
  var piece: Piece = Empty
    private set

  /**
   * @return If the piece was updated. If this returns `true` [piece] is guaranteed to be of type
   * [pieceType].
   */
  @JsonSetter("pieceType")
  fun <T : Piece> setPiece(pieceType: KClass<out T>, init: T.() -> Unit = { }): Boolean {
    require(!pieceType.isAbstract) { "Cannot set the piece to an abstract piece" }
    val pieceToPlace = pieceType.createInstance(this)

    if (pieceToPlace.place(this)) {
      piece = pieceToPlace
      require(pieceToPlace is Empty || pieceToPlace.data === pieceToPlace.data.piece.data) {
        "Pieces data does not point to this!"
      }
      pieceToPlace.init()
      return true
    }
    return false
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

  // /////////////////
  // serialization //
  // /////////////////

  @JsonGetter("pieceType")
  fun getPieceTypeName() = piece::class.qualifiedName

  @JsonSetter("pieceType")
  fun setPieceFromTypeName(typeName: String?) {
    setPiece(PIECES_MAP[typeName] ?: error("Unknown piece with the name $typeName"))
  }

  fun copy(): HexagonData {
    if (edge) return this
    return HexagonData(edge, isOpaque, isPassable).also {
      it.team = team
      it.piece = piece.copyTo(this)
    }
  }

  override fun toString(): String {
    return "team: $team piece: $piece"
  }

  companion object {

    const val BRIGHTNESS = 0.9f

    /**
     * mouse hovering over, add this to the current hex under the mouse
     */
    const val SELECTED = 0.1f

    private const val EXPECTED_NEIGHBORS = 6

    fun Island.isEdgeHexagon(hex: Hexagon<HexagonData>) = grid.getNeighborsOf(hex).size != EXPECTED_NEIGHBORS

    val EDGE_DATA = HexagonData(edge = true)
  }
}
