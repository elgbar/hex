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
import no.elg.hex.Settings
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

  init {
    resize(Gdx.graphics.width, Gdx.graphics.height)
  }

  private val topCenter: Array<ScreenText> = arrayOf(
    VariableScreenText({ "Turn ${screen.island.turn}" }),
    IfScreenText {
      if (screen.inputProcessor.cheating) {
        StaticScreenText("Cheating enabled!", color = Color.GOLD)
      } else {
        emptyText()
      }
    }
  )
  private val topRight: Array<ScreenText>
  private val topRightDbg: Array<ScreenText>

  private val topRightOffset: Int
  private val topRightDbgOffset: Int
  private val historyOffset: Int

  init {

    val treasuryText = IfScreenText {
      val selected = screen.island.selected
      if (selected != null && screen.island.isCurrentTeamHuman()) {
        StaticScreenText(
          "Treasury: ",
          next = signColoredText(selected.capital::balance) { "%d".format(it) }
        )
      } else {
        emptyText()
      }
    }

    val incomeText = IfScreenText {
      val selected = screen.island.selected
      if (selected != null && screen.island.isCurrentTeamHuman()) {
        StaticScreenText(
          "Estimated income: ",
          next = signColoredText(selected::income) { "%+d".format(it) }
        )
      } else {
        emptyText()
      }
    }

    topRight = arrayOf(
      treasuryText,
      incomeText
    )
    topRightDbg = if (Hex.debug) {
      arrayOf(
        StaticScreenText("Holding: ", next = nullCheckedText(screen.island::hand, color = YELLOW)),
        StaticScreenText(
          "Holding the edge piece: ",
          next = booleanText(callable = { screen.island.hand?.piece?.data === EDGE_DATA })
        ),
        StaticScreenText(
          "Held is edge piece: ",
          next = booleanText(callable = { screen.island.hand?.piece?.data?.edge == true })
        )
      )
    } else {
      emptyArray()
    }
    topRightOffset = 1
    topRightDbgOffset = topRightOffset + topRight.size + 1
    historyOffset = topRightDbgOffset + topRightDbg.size + 1
  }

  override fun frameUpdate() {
    batch.color = WHITE
    batch.use { batch ->

      if (screen.island.isCurrentTeamHuman()) {
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

          batch.draw(
            Hex.assets.hand,
            (Gdx.graphics.width - handWidth / 4f) / 2f,
            (height + height / 2) / 2f,
            handWidth,
            height
          )
          batch.draw(region, Gdx.graphics.width / 2f, height / 2f, width, height)
        }
      }
    }

    ScreenRenderer.drawAll(*topCenter, position = TOP_CENTER)
    ScreenRenderer.drawAll(*topRight, position = TOP_RIGHT, lineOffset = topRightOffset)
    if (Hex.debug && screen.island.isCurrentTeamHuman() && Settings.enableDebugHUD) {
      ScreenRenderer.drawAll(*topRightDbg, position = TOP_RIGHT, lineOffset = topRightDbgOffset)
      // due to the dynamic nature of history the array must be created each time

      val history = screen.island.history
      val centerRight = Array(history.historyNotes.size) { i ->
        staticTextPool.obtain().also { sst ->
          sst.text = history.historyNotes[i]
          sst.color = if (i == screen.island.history.historyPointer) YELLOW else WHITE
        }
      }
      ScreenRenderer.drawAll(*centerRight, position = TOP_RIGHT, lineOffset = historyOffset)
      // Return them to the pool after use
      for (i in 0 until (history.historyNotes.size)) {
        staticTextPool.free(centerRight[i] as StaticScreenText)
      }
    }
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