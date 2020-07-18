package no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.defaults.DefaultSatelliteData

data class HexagonData(
  /**
   * Modifier of how bright the hex should be
   */
  val brightness: Float = BRIGHT,

  val team: Team = Team.values().random(),

  /**
   * Edge hexagons are hexagons along the edge of the grid. Due to how hexagon detection works these hexagon would be
   * returned even when the mouse is not inside the hexagon In order to prevent that and gain pixel perfect hexagon
   * accuracy the player should not know these exists.
   *
   * @see no.elg.hex.util.getHexagon
   * @see no.elg.hex.hexagon.renderer.OutlineRenderer
   * @see no.elg.hex.hexagon.renderer.VerticesRenderer
   */
  val edge: Boolean
) : DefaultSatelliteData() {

  val color: Color = team.color//randomColor()//
  val type: HexType = team.type

  override var isOpaque: Boolean = false
    get() = edge or field

  override var isPassable: Boolean = true
    get() = edge and field

  companion object {
    /* Shade brightness modifier for hexagons */ //Cannot move to
    const val DIM = 0.75f

    //Can move to
    const val BRIGHT = 0.9f

    //mouse hovering over, add this to the current hex under the mouse
    const val SELECTED = 0.1f

    private const val EXPECTED_NEIGHBORS = 6

    fun isEdgeHexagon(hex: Hexagon<HexagonData>) = Hex.island.grid.getNeighborsOf(hex).size != EXPECTED_NEIGHBORS
  }
}
