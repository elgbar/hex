package no.elg.hex.hexagon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import no.elg.hex.Hex
import no.elg.hex.event.Events
import no.elg.hex.event.events.HexagonChangedPieceEvent
import no.elg.hex.event.events.HexagonChangedTeamEvent
import no.elg.hex.island.Island
import no.elg.hex.util.createInstance
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.contract.SatelliteData
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class)
class HexagonData(
  /**
   * If this hexagon is disabled, meaning it will not be a part of the playable island
   */
  disabled: Boolean,
  /**
   * The initial team of this hexagon
   */
  team: Team? = null,
  /**
   * Edge hexagons are hexagons along the edge of the grid. Due to how hexagon detection works
   * these hexagon would be returned even when the mouse is not inside the hexagon. In order to
   * prevent that and gain pixel perfect hexagon accuracy the player should not know these exist.
   *
   * @see no.elg.hex.util.getHexagon
   * @see no.elg.hex.renderer.OutlineRenderer
   * @see no.elg.hex.renderer.VerticesRenderer
   */
  val edge: Boolean = false
) : SatelliteData {

  @JsonAlias("isOpaque")
  override var isDisabled: Boolean = disabled
    set(disable) {
      require(Hex.mapEditor) { "Cannot disable hexagons in game" }
      if (field != disable) {
        field = disable
        Hex.island?.updateHexagonVisibility(this)
        Gdx.graphics.requestRendering()
      }
    }

  var team: Team = team ?: if (Hex.mapEditor) Team.entries.toTypedArray().random() else Team.STONE
    set(value) {
      if (field === value) return

      val old = field
      field = value
      if (visible) {
        Events.fireEvent(HexagonChangedTeamEvent(this, old, value))
      }
      Gdx.graphics.requestRendering()
    }

  @JsonIgnore
  var piece: Piece = Empty
    @Deprecated("Do not set directly! Use [setPiece]")
    set(value) {
      if (field === value) return

      val old = field
      field = value

      if (visible) {
        Events.fireEvent(HexagonChangedPieceEvent(this, old, value))
      }
      Gdx.graphics.requestRendering()
    }

  /**
   * @return If the piece was updated. If this returns `true` [piece] is guaranteed to be of type [T].
   */
  inline fun <reified T : Piece> setPiece(crossinline init: (T) -> Unit = { }): Boolean {
    contract { callsInPlace(init, InvocationKind.AT_MOST_ONCE) }
    return setPiece(T::class, init)
  }

  inline fun <T : Piece> setPiece(pieceType: KClass<out T>, init: (T) -> Unit = { }): Boolean {
    contract { callsInPlace(init, InvocationKind.AT_MOST_ONCE) }
    require(!pieceType.isAbstract) { "Cannot set the piece to an abstract piece" }
    val pieceToPlace = pieceType.createInstance(this)

    if (pieceToPlace.place(this)) {
      @Suppress("DEPRECATION") // OK to set directly here, this is the method we reference in the deprecation message!
      this.piece = pieceToPlace
      require(pieceToPlace is Empty || pieceToPlace.data === pieceToPlace.data.piece.data) { "Pieces data does not point to this!" }
      init(pieceToPlace)
      Gdx.graphics.requestRendering()
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
    get() = edge || isDisabled

  @get:JsonIgnore
  val visible: Boolean
    get() = !invisible

  // /////////////////
  // serialization //
  // /////////////////

  @field:JsonSetter("pieceType")
  internal var loadedTypeName: String? = null

  @field:JsonSetter("data")
  internal var serializationDataToLoad: Any? = null

  @JsonGetter("pieceType")
  fun getPieceTypeName() = if (piece == Empty) null else piece::class.simpleName

  @JsonGetter("data")
  private fun getSerializationData() = piece.serializationData

  fun copy(): HexagonData {
    if (edge) return this
    return HexagonData(isDisabled, team).also {
      @Suppress("DEPRECATION") // OK to set directly when we
      it.piece = piece.copyTo(it)
    }
  }

  override fun toString(): String {
    return "team: $team piece: $piece"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as HexagonData

    if (edge != other.edge) return false
    if (team != other.team) return false
    return piece.equalsWithoutData(other.piece)
  }

  fun equalsWithoutData(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as HexagonData

    if (edge != other.edge) return false
    if (team != other.team) return false
    return piece.equalsWithoutData(other.piece)
  }

  override fun hashCode(): Int {
    var result = edge.hashCode()
    result = 31 * result + team.hashCode()
    result = 31 * result + piece.hashCode()
    return result
  }

  companion object {

    const val BRIGHTNESS = 0.9f

    /**
     * mouse hovering over, add this to the current hex under the mouse
     */
    const val SELECTED = 0.1f

    private const val EXPECTED_NEIGHBORS = 6

    fun Island.isEdgeHexagon(hex: Hexagon<HexagonData>) = grid.getNeighborsOf(hex).size != EXPECTED_NEIGHBORS

    val EDGE_DATA = HexagonData(disabled = true, team = Team.SUN, edge = true)
  }
}