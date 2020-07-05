package src.no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.Point
import src.no.elg.hex.dim
import src.no.elg.hex.hexagon.renderer.VerticesRenderer
import com.badlogic.gdx.utils.FloatArray as GdxFloatArray

//   2-----1
//  /       \
// 3    6    0
//  \       /
//   4-----5
/**
 * @author karl henrik
 */
enum class HexType(vararg surfaces: Surface) {
  ASYMMETRICAL(
    Surface(0, 2, 1, 0.69f),  //   4-----5
    Surface(0, 3, 2, 0.79f),  //  / ‾-__  \
    Surface(0, 5, 4, 0.89f),  // 3------‾--0
    Surface(0, 4, 3, 0.98f)),  //  \  _--‾ /

  //   2-----1
  FLAT(Surface(0, 1, 5, 1.00f),  //   4-----5
    Surface(1, 4, 5, 1.00f),  //  /|   / |\
    Surface(1, 2, 4, 1.00f),  // 3 |  /  | 0
    Surface(2, 3, 4, 1.00f)),  //  \| /   |/

  //   2-----1
  HALF(Surface(0, 1, 5, 0.78f),   //   4-----5
    Surface(1, 2, 5, 0.78f),     //  /|   / |\
    Surface(2, 4, 5, 0.98f),     // 3 |  /  | 0
    Surface(2, 3, 4, 0.98f)),    //  \| /   |/

  //   2-----1
  TRIANGULAR(Surface(0, 1, 5, 0.64f),  //   4-----5
    Surface(1, 3, 5, 0.78f),  //  / _--‾ |\
    Surface(1, 2, 3, 0.83f),  // 3-‾     | 0
    Surface(3, 4, 5, 0.98f)),  //  \ ‾--_ |/

  //   2-----1
  CUBE(Surface(6, 1, 0, 0.68f),  //   4-----5
    Surface(6, 0, 5, 0.68f),  //  /  \ /  \
    Surface(6, 1, 2, 0.83f),  // 3----6----0
    Surface(6, 2, 3, 0.83f),  //  \  / \  /
    Surface(6, 5, 4, 0.98f),  //   2-----1
    Surface(6, 3, 4, 0.98f)),

  /**
   * ```
   *   4-----5
   *  /  \ /  \
   * 3----6----0
   *  \  / \  /
   *   2-----1
   * ```
   */
  HOURGLASS(Surface(6, 2, 3, 0.81f),
    Surface(6, 0, 5, 0.81f),
    Surface(6, 1, 2, 0.61f),
    Surface(6, 1, 0, 0.61f),
    Surface(6, 5, 4, 0.98f),
    Surface(6, 3, 4, 0.98f)),

  /**
   * ```
   *   4-----5
   *  /  \ /  \
   * 3----6----0
   *  \  / \  /
   *   2-----1
   * ```
   */
  DIAMOND(Surface(6, 1, 0, 0.65f),  //   4-----5
    Surface(6, 1, 2, 0.73f),  //  /  \ /  \
    Surface(6, 0, 5, 0.73f),  // 3----6----0
    Surface(6, 2, 3, 0.93f),  //  \  / \  /
    Surface(6, 4, 5, 0.93f),  //   2-----1
    Surface(6, 3, 4, 0.98f));

  private val surfaces: Array<out Surface>


  fun render(verticesRenderer: VerticesRenderer, color: Color, brightness: Float,
             hexagon: Hexagon<HexagonData>) {

    val points = hexagon.points.toMutableList()
    points.add(hexagon.center)
    for (sur in surfaces) {
      sur.render(verticesRenderer, color, brightness, points)
    }
  }

  private class Surface internal constructor(v1: Int, v2: Int, v3: Int, shade: Float) {
    private val v1: Int
    private val v2: Int
    private val v3: Int
    private val shade: Float
    fun appendFloatArray(p: Point, arr: GdxFloatArray) {
      arr.addAll(p.coordinateX.toFloat(), p.coordinateY.toFloat())
    }

    fun printPoint(points: List<Point>) {
      for ((coordinateX, coordinateY) in points) {
        print("$coordinateX, $coordinateY | ")
      }
      println()
    }

    fun render(verticesRenderer: VerticesRenderer, color: Color, brightness: Float,
               points: List<Point>) {
      val (coordinateX, coordinateY) = points[v1]
      vertices[0] = coordinateX.toFloat()
      vertices[1] = coordinateY.toFloat()
      val (coordinateX1, coordinateY1) = points[v2]
      vertices[2] = coordinateX1.toFloat()
      vertices[3] = coordinateY1.toFloat()
      val (coordinateX2, coordinateY2) = points[v3]
      vertices[4] = coordinateX2.toFloat()
      vertices[5] = coordinateY2.toFloat()
      verticesRenderer.drawTriangle(color.cpy().dim(shade * brightness).toFloatBits(), vertices)
    }

    companion object {
      private val vertices = FloatArray(6)
    }

    /**
     * @param v1
     * The first of the index vertex in the list
     * @param v2
     * The second of the index vertex in the list
     * @param v3
     * The third of the index vertex in the list
     * @param shade
     * How light the shade of the color should be, 1 is max 0 is min
     */
    init {
      require(!(shade < 0 || shade > 1)) { "The shade must be between 1 and 0" }
      this.v1 = v1
      this.v2 = v2
      this.v3 = v3
      this.shade = shade
    }
  }

  init {
    this.surfaces = surfaces
  }
}
