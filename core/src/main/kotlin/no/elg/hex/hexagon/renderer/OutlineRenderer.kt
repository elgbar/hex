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

/**
 * @author kheba
 */
object OutlineRenderer : FrameUpdatable, Disposable {

  private val lineRenderer: ShapeRenderer = ShapeRenderer(1000)

  private const val defaultRectLineWidth = 0.75f

  override fun frameUpdate() {

    lineRenderer.begin(Filled)
    lineRenderer.projectionMatrix = camera.combined

    val currHex = BasicInputHandler.cursorHex
    val selected = island.selected?.second

    for (hexagon in island.hexagons) {
      val points = hexagon.points
      val data = hexagon.getData()
      if (data.isOpaque || island.selected?.second?.contains(hexagon) == true) continue

      val brightness = HexagonData.BRIGHTNESS + (if (hexagon == currHex) HexagonData.SELECTED else 0f)

      lineRenderer.color = data.color.cpy().mul(brightness, brightness, brightness, 0.5f)
      for (i in points.indices) {
        val point = points[i]
        //get the next edge this edge is connected to
        val nextPoint = points[if (i == points.size - 1) 0 else i + 1]
        lineRenderer.line(
          point.coordinateX.toFloat(),
          point.coordinateY.toFloat(),
          nextPoint.coordinateX.toFloat(),
          nextPoint.coordinateY.toFloat())
      }
    }

    if (selected != null) {
      //render the selected hexagons always on top of the other hexagons to display the white line
      for (hexagon in selected) {
        val points = hexagon.points

        val brightness = HexagonData.BRIGHTNESS + if (hexagon == currHex) HexagonData.SELECTED else 0f

        val isCap = hexagon.getData().piece is Capital
        lineRenderer.color = (if (isCap) Color.BLUE else Color.WHITE).cpy().mul(brightness, brightness, brightness, 1f)
        for (i in points.indices) {
          val point = points[i]
          //get the next edge this edge is connected to
          val nextPoint = points[if (i == points.size - 1) 0 else i + 1]
          lineRenderer.rectLine(
            point.coordinateX.toFloat(),
            point.coordinateY.toFloat(),
            nextPoint.coordinateX.toFloat(),
            nextPoint.coordinateY.toFloat(), if (isCap) 2f else defaultRectLineWidth, lineRenderer.color, lineRenderer.color)
        }
      }
    }
    lineRenderer.end()
  }

  override fun dispose() {
    lineRenderer.dispose()
  }

}
