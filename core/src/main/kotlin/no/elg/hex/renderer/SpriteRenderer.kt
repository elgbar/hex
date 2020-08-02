package no.elg.hex.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.Spearman
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData

/**
 * @author Elg
 */
class SpriteRenderer(private val islandScreen: IslandScreen) : FrameUpdatable, Disposable {


  private val batch: SpriteBatch = SpriteBatch()

  private fun findSprite(regionName: String): AtlasRegion {
    val region = if (Hex.args.retro) {
      Hex.assets.originalSprites.findRegion(regionName)
    } else {
      Hex.assets.sprites.findRegion(regionName) ?: Hex.assets.originalSprites.findRegion(regionName)
    }
    region.flip(true, true)
    return region
  }

  private val pine by lazy { findSprite("pine") }
  private val palm by lazy { findSprite("palm") }
  private val capital by lazy { findSprite("village") }
  private val capitalFlag by lazy { findSprite("village0") }
  private val castle by lazy { findSprite("castle") }
  private val grave by lazy { findSprite("grave") }
  private val peasant by lazy { findSprite("man00") }
  private val spearman by lazy { findSprite("man10") }
  private val knight by lazy { findSprite("man20") }
  private val baron by lazy { findSprite("man30") }

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
        Peasant::class -> peasant
        Spearman::class -> spearman
        Knight::class -> knight
        Baron::class -> baron
        else -> continue@loop
      }

      val ratio = drawable.packedWidth.toFloat() / drawable.packedHeight.toFloat()

      val height = boundingBox.height.toFloat()
      val width = boundingBox.width.toFloat()

      //"+ width * (1f - ratio) / 2f" because we need to compensate for the removed width
      batch.draw(
        drawable,
        boundingBox.x.toFloat() + width * (1f - ratio) / 2f,
        boundingBox.y.toFloat(),
        width * ratio,
        height
      )
    }

    batch.end()
  }

  override fun dispose() {
    batch.dispose()
  }

}
