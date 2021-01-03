package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.Color.YELLOW
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.api.Resizable
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Grave
import no.elg.hex.hexagon.HexagonData.Companion.EDGE_DATA
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

  private val batch = SpriteBatch()
  private val shapeRenderer = ShapeRenderer()
  private val camera = OrthographicCamera()

  private val topCenter: Array<ScreenText> = arrayOf(
    VariableScreenText({ "Turn ${screen.island.turn}" }),
    IfScreenText {
      if (screen.inputProcessor.infiniteMoney) {
        StaticScreenText("Cheating enabled!", color = Color.GOLD)
      } else {
        emptyText()
      }
    }
  )
  private val topRight: Array<ScreenText>

  init {

    val unknownText = StaticScreenText("???", color = YELLOW)
    val treasuryText = IfScreenText {
      if (screen.island.selected != null) {
        StaticScreenText(
          "Treasury: ",
          next = signColoredText(screen.island.selected!!.capital::balance) { "%d".format(it) }
        )
      } else emptyText()
    }

    val incomeText = IfScreenText {
      if (screen.island.selected != null) StaticScreenText(
        "Estimated income: ",
        next = signColoredText(screen.island.selected!!::income) { "%+d".format(it) }
      ) else emptyText()
    }

    topRight = if (Hex.debug) {
      arrayOf(
        emptyText(),
        treasuryText,
        incomeText,
        emptyText(),
        StaticScreenText("Holding: ", next = nullCheckedText(screen.island::hand, color = YELLOW)),
        StaticScreenText("Holding the edge piece: ", next = booleanText(callable = { screen.island.hand?.piece?.data === EDGE_DATA })),
        StaticScreenText("Held is edge piece: ", next = booleanText(callable = { screen.island.hand?.piece?.data?.edge == true })),
        emptyText(),
      )
    } else {
      arrayOf(
        emptyText(),
        treasuryText,
        incomeText,
      )
    }
  }

  override fun frameUpdate() {
    batch.color = WHITE
    batch.use { batch ->

      fun calcSize(region: AtlasRegion, heightPercent: Float = 0.1f): Pair<Float, Float> {
        val height = (Gdx.graphics.height * heightPercent)
        val width = height * (region.packedWidth / region.packedHeight.toFloat())
        return width to height
      }

      screen.island.hand?.also { (_, piece) ->
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
    }

    ScreenRenderer.drawAll(*topCenter, position = TOP_CENTER)

    if (Hex.debug) {

      // due to the dynamic nature of history the array must be created each time

      val history = screen.island.history
      val centerRight = Array(topRight.size + history.historyNotes.size) { i ->
        if (i < topRight.size) {
          topRight[i]
        } else {
          val historyIndex = i - topRight.size
          staticTextPool.obtain().also { sst ->
            sst.text = history.historyNotes[historyIndex]
            sst.color = if (historyIndex == screen.island.history.historyPointer) YELLOW else WHITE
          }
        }
      }
      ScreenRenderer.drawAll(*centerRight, position = TOP_RIGHT)
      for (i in topRight.size until (topRight.size + history.historyNotes.size)) {
        staticTextPool.free(centerRight[i] as StaticScreenText)
      }
    } else {
      ScreenRenderer.drawAll(*topRight, position = TOP_RIGHT)
    }
    if (screen.island.isCurrentTeamAI()) return
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
}
