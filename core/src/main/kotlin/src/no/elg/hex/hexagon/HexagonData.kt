package src.no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import no.elg.hex.Hex
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.defaults.DefaultSatelliteData
import src.no.elg.hex.util.randomColor
import kotlin.random.Random

data class HexagonData(
  /**
   * Modifier of how bright the hex should be
   */
  val brightness: Float = DIM,

  val color: Color = randomColor(),

  val type: HexType = HexType.values()[Random.nextInt(HexType.values().size)],

  val callback: Hexagon<HexagonData>
) : DefaultSatelliteData() {

  val invisible by lazy {

    val adjacent = Hex.map.grid.getNeighborsOf(callback)
    adjacent.size != EXPECTED_NEIGHBORS
  }

  override var isOpaque: Boolean = false
    get() = invisible or field

  override var isPassable: Boolean = true
    get() = invisible and field

  companion object {
    /* Shade brightness modifier for hexagons */ //Cannot move to
    const val DIM = 0.75f

    //Can move to
    const val BRIGHT = 0.9f

    //mouse hovering over, add this to the current hex under the mouse
    const val SELECTED = 0.1f

    const val EXPECTED_NEIGHBORS = 6
  }
}
