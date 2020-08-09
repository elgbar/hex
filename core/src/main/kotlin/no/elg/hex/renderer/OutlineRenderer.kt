package no.elg.hex.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.canAttack
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

    val currHex = islandScreen.basicIslandInputProcessor.cursorHex

    fun draw(
      hexes: Iterable<Hexagon<HexagonData>>,
      color: Color?,
      alpha: Float = 1f,
      lineWidth: Float = DEFAULT_RECT_LINE_WIDTH
    ) {

      for (hexagon in hexes) {
        val points = hexagon.points
        val data = islandScreen.island.getData(hexagon)
        if (data.invisible) continue

        val brightness = HexagonData.BRIGHTNESS + (if (hexagon.cubeCoordinate == currHex?.cubeCoordinate) HexagonData.SELECTED else 0f)


        lineRenderer.color = (color ?: data.color).cpy().mul(brightness, brightness, brightness, alpha)

        for (i in points.indices) {
          val point = points[i]
          //get the next edge this edge is connected to
          val nextPoint = points[(i + 1) % points.size]
          lineRenderer.rectLine(
            point.coordinateX.toFloat(),
            point.coordinateY.toFloat(),
            nextPoint.coordinateX.toFloat(),
            nextPoint.coordinateY.toFloat(), lineWidth, lineRenderer.color, lineRenderer.color)
        }
      }
    }

    draw(islandScreen.island.hexagons, null)

    islandScreen.island.selected?.also {
      draw(it.hexagons, Color.WHITE)

      val hand = islandScreen.island.inHand
      if (hand != null && hand.piece is LivingPiece) {
        val hexes = it.enemyBorderHexes.filter { hex ->
          islandScreen.island.canAttack(hex, hand.piece)
        }
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
