package no.elg.hex.hexagon.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex.camera
import no.elg.hex.Hex.island
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.input.BasicInputHandler
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

/**
 * @author kheba
 */
object OutlineRenderer : FrameUpdatable, Disposable {

  private val lineRenderer: ShapeRenderer = ShapeRenderer(1000)

  private const val DEFAULT_RECT_LINE_WIDTH = 0.75f

  override fun frameUpdate() {

    lineRenderer.begin(Filled)
    lineRenderer.projectionMatrix = camera.combined

    val currHex = BasicInputHandler.cursorHex


    fun draw(
      hexes: Iterable<Hexagon<HexagonData>>,
      alpha: Float,
      dataIndependent: Boolean,
      format: (data: HexagonData) -> Pair<Color, Float>
    ) {

      val dataIndependentFormat = format(HexagonData.EDGE_DATA)

      for (hexagon in hexes) {
        val points = hexagon.points
        val data = hexagon.getData()
        if (data.invisible) continue

        val brightness = HexagonData.BRIGHTNESS + (if (hexagon.cubeCoordinate == currHex?.cubeCoordinate) HexagonData.SELECTED else 0f)

        val (color, width) = if (dataIndependent) dataIndependentFormat else format(data)
        lineRenderer.color = color.cpy().mul(brightness, brightness, brightness, alpha)

        for (i in points.indices) {
          val point = points[i]
          //get the next edge this edge is connected to
          val nextPoint = points[if (i == points.size - 1) 0 else i + 1]
          lineRenderer.rectLine(
            point.coordinateX.toFloat(),
            point.coordinateY.toFloat(),
            nextPoint.coordinateX.toFloat(),
            nextPoint.coordinateY.toFloat(), width, lineRenderer.color, lineRenderer.color)
        }
      }
    }

    draw(island.hexagons, 0.5f, false) { data ->
      data.color to DEFAULT_RECT_LINE_WIDTH
    }

    island.selected?.also { (_, hexagons) ->
      draw(hexagons, 1f, true) {
        Color.WHITE to DEFAULT_RECT_LINE_WIDTH
      }
    }

    draw(island.hexagons.filter { it.getData().piece is Capital }, 1f, true) {
      Color.BLUE to DEFAULT_RECT_LINE_WIDTH * 3f
    }

    lineRenderer.end()
  }

  override fun dispose() {
    lineRenderer.dispose()
  }

}
