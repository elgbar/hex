package no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import no.elg.hex.Hex
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.defaults.DefaultSatelliteData

@JsonInclude(NON_DEFAULT)
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator::class)
data class HexagonData(

  /**
   * Modifier of how bright the hex should be
   */
  var brightness: Float = BRIGHT,

  @JsonInclude(ALWAYS)
  var team: Team = Team.values().random(),

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

  @JsonIgnore
  val color: Color = team.color

  @JsonIgnore
  val type: HexType = team.type


  companion object {
    /* Shade brightness modifier for hexagons */ //Cannot move to
    const val DIM = 0.75f

    //Can move to
    const val BRIGHT = 0.9f

    //mouse hovering over, add this to the current hex under the mouse
    const val SELECTED = 0.1f

    private const val EXPECTED_NEIGHBORS = 6

    fun isEdgeHexagon(hex: Hexagon<HexagonData>) = Hex.island.grid.getNeighborsOf(hex).size != EXPECTED_NEIGHBORS

    val EDGE_DATA = HexagonData(edge = true)
  }

  override fun toString(): String {
    return "opaque: %-5s, edge: %-5s, team: $team".format(isOpaque, edge)
  }
}
