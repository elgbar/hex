package no.elg.hex.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

/**
 * @author kheba
 */
class OutlineRenderer(private val islandScreen: IslandScreen) : FrameUpdatable, Disposable {

  private val lineRenderer: ShapeRenderer = ShapeRenderer(1000)

  override fun frameUpdate() {

    lineRenderer.begin(Filled)
    lineRenderer.projectionMatrix = islandScreen.camera.combined

    val currHex = islandScreen.basicInputProcessor.cursorHex

    fun draw(
      hexes: Iterable<Hexagon<HexagonData>>,
      alpha: Float,
      white: Boolean
    ) {

      for (hexagon in hexes) {
        val points = hexagon.points
        val data = hexagon.getData(islandScreen.island)
        if (data.invisible) continue

        val brightness = HexagonData.BRIGHTNESS + (if (hexagon.cubeCoordinate == currHex?.cubeCoordinate) HexagonData.SELECTED else 0f)

        val color = if (white) Color.WHITE else data.color

        lineRenderer.color = color.cpy().mul(brightness, brightness, brightness, alpha)

        for (i in points.indices) {
          val point = points[i]
          //get the next edge this edge is connected to
          val nextPoint = points[(i + 1) % points.size]
          lineRenderer.rectLine(
            point.coordinateX.toFloat(),
            point.coordinateY.toFloat(),
            nextPoint.coordinateX.toFloat(),
            nextPoint.coordinateY.toFloat(), DEFAULT_RECT_LINE_WIDTH, lineRenderer.color, lineRenderer.color)
        }
      }
    }

    draw(islandScreen.island.hexagons, 1f, false)

    islandScreen.island.selected?.also {
      draw(it.hexagons, 1f, true)
    }
    
    lineRenderer.end()
  }

  override fun dispose() {
    lineRenderer.dispose()
  }

  companion object {
    private const val DEFAULT_RECT_LINE_WIDTH = 0.75f
  }

}
