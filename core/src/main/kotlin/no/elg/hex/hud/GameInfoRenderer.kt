package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.api.Resizable
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Grave
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.Spearman
import no.elg.hex.hud.ScreenDrawPosition.TOP_CENTER
import no.elg.hex.hud.ScreenDrawPosition.TOP_RIGHT
import no.elg.hex.screens.PlayableIslandScreen

/** @author Elg */
class GameInfoRenderer(private val screen: PlayableIslandScreen) : FrameUpdatable, Resizable, Disposable {

  private val rightInfo = ArrayList<ScreenText>()
  private val unprojectVector = Vector3()
  private val batch: SpriteBatch = SpriteBatch()
  private val shapeRenderer = ShapeRenderer()
  private val camera = OrthographicCamera()

  override fun frameUpdate() {

    ScreenRenderer.drawAll(ScreenText("Turn ${screen.island.turn}", bold = true), position = TOP_CENTER)
    if (screen.inputProcessor.infiniteMoney) {
      ScreenRenderer.drawAll(emptyText(), CHEATING_SCREEN_TEXT, position = TOP_CENTER)
    }

    rightInfo.clear()

    if (screen.island.currentAI == null) {

      screen.island.selected?.also { selected ->
        rightInfo += emptyText()
        rightInfo += ScreenText("Treasury: ", next = signColoredText(selected.capital.balance) { "%d".format(it) })
        rightInfo += ScreenText("Estimated income: ", next = signColoredText(selected.income) { "%+d".format(it) })
        if (Hex.debug) {
          rightInfo += emptyText()
          rightInfo += ScreenText("Holding: ", next = nullCheckedText(screen.island.inHand, color = Color.YELLOW))
          rightInfo += ScreenText("Holding edge piece: ", next = booleanText(screen.island.inHand?.piece?.data === HexagonData.EDGE_DATA))
          rightInfo += emptyText()
        }
      }

      batch.color = WHITE
      batch.begin()

      fun calcSize(region: AtlasRegion, heightPercent: Float = 0.1f): Pair<Float, Float> {
        val height = (Gdx.graphics.height * heightPercent)
        val width = height * (region.packedWidth / region.packedHeight.toFloat())
        return width to height
      }

      screen.island.inHand?.also { (_, piece) ->
        val region: AtlasRegion =
          when (piece) {
            is Capital -> Hex.assets.capital
            is PalmTree -> Hex.assets.palm
            is PineTree -> Hex.assets.pine
            is Castle -> Hex.assets.castle
            is Grave -> Hex.assets.grave
            is Peasant -> Hex.assets.peasant.getKeyFrame(0f)
            is Spearman -> Hex.assets.spearman.getKeyFrame(0f)
            is Knight -> Hex.assets.knight.getKeyFrame(0f)
            is Baron -> Hex.assets.baron.getKeyFrame(0f)
            is Empty -> return@also
          }

        val (width, height) = calcSize(region)
        val handWidth = height * (Hex.assets.hand.originalWidth / Hex.assets.hand.originalHeight.toFloat())

        batch.draw(Hex.assets.hand, (Gdx.graphics.width - handWidth / 4f) / 2f, (height + height / 2) / 2f, handWidth, height)
        batch.draw(region, Gdx.graphics.width / 2f, height / 2f, width, height)
      }
      batch.end()
    }

    if (Hex.debug) {
      val selected = screen.island.history.historyPointer
      rightInfo.addAll(screen.island.history.historyNotes.mapIndexed { i, it -> ScreenText(it, color = if (i == selected) Color.YELLOW else WHITE) })
    }
    ScreenRenderer.drawAll(*rightInfo.toTypedArray(), position = TOP_RIGHT)
  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(true)
    batch.projectionMatrix = camera.combined
    shapeRenderer.projectionMatrix = camera.combined
  }

  override fun dispose() {
    batch.dispose()
    shapeRenderer.dispose()
  }

  companion object {
    val CHEATING_SCREEN_TEXT = ScreenText("Cheating enabled!", color = Color.GOLD)
  }
}
