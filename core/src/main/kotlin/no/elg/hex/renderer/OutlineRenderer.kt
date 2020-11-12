package no.elg.hex.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.canAttack
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.Hexagon

/** @author kheba */
class OutlineRenderer(private val islandScreen: PreviewIslandScreen) :
  FrameUpdatable, Disposable {

  private val lineRenderer: ShapeRenderer = ShapeRenderer(1000)

  override fun frameUpdate() {

    lineRenderer.begin(Filled)
    lineRenderer.projectionMatrix = islandScreen.camera.combined

    val currHex = islandScreen.cursorHexagon

    fun draw(
      hexes: Iterable<Hexagon<HexagonData>>,
      color: Color?,
      alpha: Float,
      lineWidth: Float = DEFAULT_RECT_LINE_WIDTH
    ) {

      for (hexagon in hexes) {
        val points = hexagon.points
        val data = islandScreen.island.getData(hexagon)
        val drawEdges = data.edge && Hex.args.`draw-edges`

        if (data.invisible && !drawEdges) continue

        val brightness =
          HexagonData.BRIGHTNESS +
            (if (hexagon.cubeCoordinate == currHex?.cubeCoordinate) HexagonData.SELECTED
            else 0f)

        if (drawEdges) {
          lineRenderer.color.set(Color.WHITE)
        } else {
          lineRenderer.color.set(color ?: data.color).mul(brightness, brightness, brightness, alpha)
        }

        for (i in points.indices) {
          val point = points[i]
          // get the next edge this edge is connected to
          val nextPoint = points[(i + 1) % points.size]
          lineRenderer.rectLine(
            point.coordinateX.toFloat(),
            point.coordinateY.toFloat(),
            nextPoint.coordinateX.toFloat(),
            nextPoint.coordinateY.toFloat(),
            lineWidth,
            lineRenderer.color,
            lineRenderer.color)
        }
      }
    }

    draw(islandScreen.island.hexagons, null, 0.25f)

    islandScreen.island.selected?.also {
      draw(it.hexagons, Color.WHITE, 1f)

      val hand = islandScreen.island.inHand
      if (hand != null && hand.piece is LivingPiece) {
        val hexes =
          it.enemyBorderHexes.filter { hex -> islandScreen.island.canAttack(hex, hand.piece) }
        draw(hexes, Color.RED, 1f, 2f)
      }
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
