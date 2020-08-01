package no.elg.hex.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.PineTree
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData

/**
 * @author Elg
 */
class SpriteRenderer(private val islandScreen: IslandScreen) : FrameUpdatable, Disposable {


  private val batch: SpriteBatch = SpriteBatch()
  private val pine: AtlasRegion by lazy { Hex.assets.sprites.findRegion("pine") }
  private val palm: AtlasRegion by lazy { Hex.assets.sprites.findRegion("palm") }
  private val capital: AtlasRegion by lazy { Hex.assets.sprites.findRegion("capital") }
  private val capitalFlag: AtlasRegion by lazy { Hex.assets.sprites.findRegion("capital_flag") }
  private val castle: AtlasRegion by lazy { Hex.assets.sprites.findRegion("castle") }
  private val grave: AtlasRegion by lazy { Hex.assets.sprites.findRegion("grave") }

  override fun frameUpdate() {

    val currHex = islandScreen.basicInputProcessor.cursorHex

    batch.projectionMatrix = islandScreen.camera.combined

    batch.begin()

    loop@ for (hexagon in islandScreen.island.hexagons) {
      val data = hexagon.getData(islandScreen.island)
      if (data.invisible) continue

      val brightness = HexagonData.BRIGHTNESS + (if (hexagon.cubeCoordinate == currHex?.cubeCoordinate) HexagonData.SELECTED else 0f)

      batch.setColor(brightness, brightness, brightness, 1f)

      val boundingBox = hexagon.internalBoundingBox

      val drawable = when (data.piece::class) {
        Capital::class -> {
          val piece = (data.piece as Capital)
          val territoryHexes = islandScreen.island.getTerritoryHexagons(hexagon) ?: continue@loop
          if (piece.calculateIncome(territoryHexes, islandScreen.island) < 10) capital else capitalFlag
        }
        PalmTree::class -> palm
        PineTree::class -> pine
        Castle::class -> castle
        else -> continue@loop
      }

      val ratio = drawable.packedWidth.toFloat() / drawable.packedHeight.toFloat()

      val height = boundingBox.height.toFloat()
      val width = boundingBox.width.toFloat()

      //"- width * (1f - ratio) / 2f" because we need to compensate for the removed width
      batch.draw(
        drawable,
        boundingBox.x.toFloat(),
        boundingBox.y.toFloat(),
        (width - width * (1f - ratio) / 2f) / 2f,
        height / 2f,
        width * ratio,
        height,
        1f,
        1f,
        180f
      )

    }

    batch.end()
  }

  override fun dispose() {
  }

}
