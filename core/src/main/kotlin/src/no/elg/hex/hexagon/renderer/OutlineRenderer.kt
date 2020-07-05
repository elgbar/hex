package src.no.elg.hex.hexagon.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex.camera
import no.elg.hex.Hex.world
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.Point
import src.no.elg.hex.FrameUpdatable
import src.no.elg.hex.hexagon.HexUtil.getData
import src.no.elg.hex.hexagon.HexUtil.getHexagons
import src.no.elg.hex.hexagon.HexagonData
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

    val drawnEdges = HashMap<Vector2, HashSet<Vector2>>()

    for (hex in getHexagons(grid)) {
      val edges = getEdges(hex)
      lineRenderer.color = getData(hex).color
      for (i in edges.indices) {
        val edge = edges[i]
        drawnEdges.putIfAbsent(edge, HashSet())
        val connected: HashSet<Vector2> = drawnEdges[edge] ?: throw IllegalStateException("No empty set provided!")

        //get the next edge this edge is connected to
        val edgeTo = edges[if (i == edges.size - 1) 0 else i + 1]
        if (!connected.contains(edgeTo)) {
          connected.add(edgeTo)
          lineRenderer.line(edge, edgeTo)
        }
      }
    }
    lineRenderer.end()
  }

  fun getEdges(hex: Hexagon<HexagonData>): Array<Vector2> {
    val points = hex.points
    return Array(6) { i -> toVector(points[i]) }
  }

  private fun toVector(p: Point): Vector2 {
    return Vector2(p.coordinateX.toFloat(), p.coordinateY.toFloat())
  }

  fun drawLine(start: Vector2, end: Vector2, color: Color) {
    lineRenderer.begin(Line)
    lineRenderer.color = color
    lineRenderer.line(start, end)
    lineRenderer.end()
  }

  override fun dispose() {
    lineRenderer.dispose()
  }

}
