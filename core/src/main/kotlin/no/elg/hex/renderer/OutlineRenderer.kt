package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.actionableHexagons
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.canAttack
import no.elg.hex.util.getData
import no.elg.hex.util.requestRenderingIn
import no.elg.hex.util.safeUse
import org.hexworks.mixite.core.api.Hexagon

/** @author kheba */
class OutlineRenderer(private val islandScreen: PreviewIslandScreen) :
  FrameUpdatable,
  Disposable {

  private val lineRenderer: ShapeRenderer = ShapeRenderer(1000)

  private var elapsedAnimationTime = 0f
  private val attackableAnimation =
    Animation(ATTACKABLE_OUTLINE_BLINK_PERIOD_SECONDS, 2.25f, 1f).also {
      it.playMode = Animation.PlayMode.LOOP
    }

  private val allowedToDrawInvisible get() = !islandScreen.isPreviewRenderer
  private val shouldDrawEdges get() = Hex.args.`draw-edges` && allowedToDrawInvisible
  private val shouldDrawInvisible get() = Hex.mapEditor && allowedToDrawInvisible

  override fun frameUpdate() {
    lineRenderer.safeUse(Filled, islandScreen.camera) {
      Gdx.gl.glEnable(GL20.GL_BLEND)
      Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

      val island = islandScreen.island
      val hexagonsToRender = if (shouldDrawEdges || shouldDrawInvisible) island.allHexagons else island.visibleHexagons
      drawOutLines(hexagonsToRender) { hexagon, target ->
        val data = island.getData(hexagon)
        if (data.edge) {
          if (shouldDrawEdges) target.set(Color.WHITE) else target.set(Color.CLEAR)
        } else if (shouldDrawInvisible && data.invisible) {
          target.set(Color.GRAY)
        } else {
          target.set(data.color)
          target.a = 0.5f
        }
      }

      val shouldDrawCurrentTeamHexagons = (island.isCurrentTeamHuman() && !Hex.mapEditor && allowedToDrawInvisible) || (Hex.debug && Settings.debugAIAction)
      if (shouldDrawCurrentTeamHexagons) {
        island.selected?.also {
          drawOutLines(it.hexagons) { _, target -> target.set(selectedColor) }

          val hand = island.hand

          if (hand != null && hand.piece is LivingPiece) {
            elapsedAnimationTime += Gdx.graphics.deltaTime
            val attackableLineWidth = attackableAnimation.getKeyFrame(elapsedAnimationTime)
            val attackableHexes = it.enemyBorderHexes.filter { hex -> island.canAttack(hex, hand.piece) }
            drawOutLines(attackableHexes, attackableLineWidth) { hexagon, target ->
              val str = island.calculateStrength(hexagon)
              target.set(attackColor(hand.piece.strength - str))
            }
            Gdx.graphics.requestRenderingIn(ATTACKABLE_OUTLINE_BLINK_PERIOD_SECONDS)
          } else {
            elapsedAnimationTime = 0f
          }
        }

        if (Settings.enableActionHighlight || islandScreen.tempShowActionToDo) {
          drawOutLines(island.actionableHexagons().toSet()) { _, target -> target.set(Color.GOLD) }
        }
      }
    }
  }

  private inline fun drawOutLines(
    hexes: Iterable<Hexagon<HexagonData>>,
    lineWidth: Float = DEFAULT_RECT_LINE_WIDTH,
    color: (hexagon: Hexagon<HexagonData>, target: Color) -> Unit
  ) {
    for (hexagon in hexes) {
      val points = hexagon.points
      color(hexagon, lineRenderer.color)

      for (i in points.indices) {
        val point = points[i]
        // get the next edge this edge is connected to
        val nextPoint = points[(i + 1) % points.size]
        lineRenderer.rectLine(
          point.coordinateX.toFloat(),
          point.coordinateY.toFloat(),
          nextPoint.coordinateX.toFloat(),
          nextPoint.coordinateY.toFloat(),
          lineWidth
        )
      }
    }
  }

  override fun dispose() {
    lineRenderer.dispose()
  }

  companion object {
    private const val DEFAULT_RECT_LINE_WIDTH = 1f

    private const val ATTACKABLE_OUTLINE_BLINK_PERIOD_SECONDS = .75f

    fun attackColor(str: Int): Color =
      when (str) {
        1 -> attackColor
        2 -> attackColor2
        3 -> attackColor3
        else -> attackColor4
      }

    private val attackColor: Color = Color.valueOf("#FF0000")
    private val attackColor2: Color = Color.valueOf("#FF3D3D")
    private val attackColor3: Color = Color.valueOf("#FF8484")
    private val attackColor4: Color = Color.valueOf("#FFAEAE")

    val selectedColor: Color = Color.valueOf("#EDEDED")
  }
}