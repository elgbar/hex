package no.elg.hex.hexagon

import com.badlogic.gdx.graphics.Color
import no.elg.hex.renderer.VerticesRenderer
import no.elg.hex.util.dim
import org.hexworks.mixite.core.api.Hexagon

/**
 *
 * Different ways to render hexagons with six points
 *
 * ```
 *   4----5
 *  /      \
 * 3    6   0
 *  \      /
 *   2----1
 * ```
 * @author karl henrik
 */
enum class HexType(private vararg val surfaces: Surface) {

  /**
   * ```
   *    4-----5
   *   / ‾-__  \
   *  3------‾--0
   *   \  _--‾ /
   *    2-----1
   * ```
   */
  ASYMMETRICAL(
    Surface(0, 2, 1, 0.69f),
    Surface(0, 3, 2, 0.79f),
    Surface(0, 5, 4, 0.89f),
    Surface(0, 4, 3, 0.98f)
  ),

  /**
   * ```
   *    4-----5
   *   /|   / |\
   *  3 |  /  | 0
   *   \| /   |/
   *    2-----1
   * ```
   */
  FLAT(
    Surface(0, 1, 5, 1.00f),
    Surface(1, 4, 5, 1.00f),
    Surface(1, 2, 4, 1.00f),
    Surface(2, 3, 4, 1.00f)
  ),

  /**
   * ```
   *    4-----5
   *   /|   / |\
   *  3 |  /  | 0
   *   \| /   |/
   *    2-----1
   * ```
   */
  HALF(
    Surface(0, 1, 5, 0.78f),
    Surface(1, 2, 5, 0.78f),
    Surface(2, 4, 5, 0.98f),
    Surface(2, 3, 4, 0.98f)
  ),

  /**
   * ```
   *    4-----5
   *   / ‾-__  \
   *  3------‾--0
   *   \  _--‾ /
   *    2-----1
   * ```
   */
  TRIANGULAR(
    Surface(0, 1, 5, 0.64f),
    Surface(1, 3, 5, 0.78f),
    Surface(1, 2, 3, 0.83f),
    Surface(3, 4, 5, 0.98f)
  ),

  /**
   *
   * ```
   *   4-----5
   *  /  \ /  \
   * 3----6----0
   *  \  / \  /
   *   2-----1
   * ```
   */
  CUBE(
    Surface(6, 1, 0, 0.68f),
    Surface(6, 0, 5, 0.68f),
    Surface(6, 1, 2, 0.83f),
    Surface(6, 2, 3, 0.83f),
    Surface(6, 5, 4, 0.98f),
    Surface(6, 3, 4, 0.98f)
  ),

  /**
   * `L` Lightest color, `M` medium dark ,and `D` darkest
   *
   * ```
   *   4-----5
   *  /L \L/ M\
   * 3----6----0
   *  \M /D\ D/
   *   2-----1
   * ```
   */
  HOURGLASS(
    Surface(6, 2, 3, 0.81f),
    Surface(6, 0, 5, 0.81f),
    Surface(6, 1, 2, 0.61f),
    Surface(6, 1, 0, 0.61f),
    Surface(6, 5, 4, 0.98f),
    Surface(6, 3, 4, 0.98f)
  ),

  /**
   * ```
   *   4-----5
   *  /  \ /  \
   * 3----6----0
   *  \  / \  /
   *   2-----1
   * ```
   */
  DIAMOND(
    Surface(6, 1, 0, 0.65f),
    Surface(6, 1, 2, 0.73f),
    Surface(6, 0, 5, 0.73f),
    Surface(6, 2, 3, 0.93f),
    Surface(6, 4, 5, 0.93f),
    Surface(6, 3, 4, 0.98f)
  );

  fun render(verticesRenderer: VerticesRenderer, color: Color, hexagon: Hexagon<HexagonData>) {
    for ((i, element) in hexagon.vertices.withIndex()) {
      points[i] = element.toFloat()
    }

    val center = hexagon.center
    points[12] = center.coordinateX.toFloat()
    points[13] = center.coordinateY.toFloat()

    for (surface in surfaces) {
      surface.render(verticesRenderer, color, points)
    }
  }

  companion object {
    val points = FloatArray(2 * 7)
  }

  private data class Surface(
    /** The first of the index vertex in the list */
    val v1: Int,
    /** The second of the index vertex in the list */
    val v2: Int,
    /** The third of the index vertex in the list */
    val v3: Int,
    /** How light the shade of the color should be, 1 is max 0 is min */
    val shade: Float
  ) {

    init {
      require(shade in 0.0..1.0) { "The shade must be between 1 and 0" }
    }

    fun render(verticesRenderer: VerticesRenderer, color: Color, points: FloatArray) {
      vertices[0] = points[v1 * 2]
      vertices[1] = points[v1 * 2 + 1]

      vertices[2] = points[v2 * 2]
      vertices[3] = points[v2 * 2 + 1]

      vertices[4] = points[v3 * 2]
      vertices[5] = points[v3 * 2 + 1]
      verticesRenderer.drawTriangle(color.cpy().dim(shade).toFloatBits(), vertices)
    }

    companion object {
      private val vertices = FloatArray(6)
    }
  }
}