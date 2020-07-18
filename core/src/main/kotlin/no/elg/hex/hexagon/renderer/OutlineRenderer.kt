package no.elg.hex.hexagon.renderer

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex.camera
import no.elg.hex.Hex.island
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Point
import java.util.HashSet

/**
 * @author kheba
 */
object OutlineRenderer : FrameUpdatable, Disposable {

  private val lineRenderer: ShapeRenderer = ShapeRenderer(1000)

  override fun frameUpdate() {

    val grid = island.grid

    lineRenderer.begin(Line)
    lineRenderer.projectionMatrix = camera.combined

    val drawnEdges = HashMap<Point, HashSet<Point>>()

    for (hex in grid.hexagons) {
      val points = hex.points
      val data = hex.getData()
      if (data.isOpaque) continue

      lineRenderer.color = data.color
      for (i in points.indices) {
        val point = points[i]
        //get the next edge this edge is connected to
        val nextPoint = points[if (i == points.size - 1) 0 else i + 1]

        drawnEdges.putIfAbsent(point, HashSet())
        val connected: HashSet<Point> = drawnEdges[point] ?: error("No empty set provided!")

        if (!connected.contains(nextPoint)) {
          connected.add(nextPoint)
          lineRenderer.line(
            point.coordinateX.toFloat(),
            point.coordinateY.toFloat(),
            nextPoint.coordinateX.toFloat(),
            nextPoint.coordinateY.toFloat()
          )
        }
      }
    }
    lineRenderer.end()
  }

  override fun dispose() {
    lineRenderer.dispose()
  }

}
