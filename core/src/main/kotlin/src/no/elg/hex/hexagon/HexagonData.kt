package src.no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import org.hexworks.mixite.core.api.defaults.DefaultSatelliteData
import src.no.elg.hex.randomColor
import kotlin.random.Random

data class HexagonData(
  /**
   * Modifier of how bright the hex should be
   */
  var brightness: Float = DIM,

  var color: Color = randomColor(),

  var type: HexType = HexType.values()[Random.nextInt(HexType.values().size)]
) : DefaultSatelliteData() {


  companion object {
    /* Shade brightness modifier for hexagons */ //Cannot move to
    var DIM = 0.75f

    //Can move to
    var BRIGHT = 0.9f

    //mouse hovering over, add this to the current hex under the mouse
    var SELECTED = 0.1f
  }
}
