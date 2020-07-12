package src.no.elg.hex.hexagon.renderer

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex.camera
import no.elg.hex.Hex.world
import org.hexworks.mixite.core.api.Point
import src.no.elg.hex.FrameUpdatable
import src.no.elg.hex.InputHandler.cameraOffsetX
import src.no.elg.hex.InputHandler.cameraOffsetY
import src.no.elg.hex.hexagon.HexUtil.getData
import java.util.HashSet

/**
 * @author kheba
 */
object OutlineRenderer : FrameUpdatable, Disposable {

  private val lineRenderer: ShapeRenderer = ShapeRenderer(1000)

  override fun frameUpdate() {

    val grid = world.grid

    lineRenderer.begin(Line)
    lineRenderer.projectionMatrix = camera.combined

    val drawnEdges = HashMap<Point, HashSet<Point>>()

    for (hex in grid.hexagons) {
      val points = hex.points

      lineRenderer.color = getData(hex).color
      for (i in points.indices) {
        val point = points[i]
        //get the next edge this edge is connected to
        val nextPoint = points[if (i == points.size - 1) 0 else i + 1]

        drawnEdges.putIfAbsent(point, HashSet())
        val connected: HashSet<Point> = drawnEdges[point] ?: throw IllegalStateException("No empty set provided!")

        if (!connected.contains(nextPoint)) {
          connected.add(nextPoint)
          lineRenderer.line(
            point.coordinateX.toFloat() + cameraOffsetX,
            point.coordinateY.toFloat() + cameraOffsetY,
            nextPoint.coordinateX.toFloat() + cameraOffsetX,
            nextPoint.coordinateY.toFloat() + cameraOffsetY
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
