package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP_PINGPONG
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.Spearman
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData
import com.badlogic.gdx.utils.Array as GdxArray

/**
 * @author Elg
 */
class SpriteRenderer(private val islandScreen: IslandScreen) : FrameUpdatable, Disposable {


  private val batch: SpriteBatch = SpriteBatch()

  companion object {
    private fun findSprite(regionName: String): AtlasRegion {
      val region = if (Hex.args.retro) {
        Hex.assets.originalSprites.findRegion(regionName)
      } else {
        Hex.assets.sprites.findRegion(regionName) ?: Hex.assets.originalSprites.findRegion(regionName)
      }
      region.flip(false, true)
      return region
    }

    private fun findAnimation(regionPrefix: String, totalFrames: Int, frameDuration: Float): Animation<AtlasRegion> {
      val array = GdxArray<AtlasRegion>()
      for (frame in 0 until totalFrames) {
        array.add(findSprite(regionPrefix + frame))
      }

      return Animation(frameDuration, array, LOOP_PINGPONG)
    }

    private val pine by lazy { findSprite("pine") }
    private val palm by lazy { findSprite("palm") }
    private val capital by lazy { findSprite("village") }
    private val capitalFlag by lazy { findAnimation("village", 8, 1 / 10f) }
    private val castle by lazy { findSprite("castle") }
    private val grave by lazy { findSprite("grave") }
    private val peasant by lazy { findAnimation("man0", 5, 1 / 17f) }
    private val spearman by lazy { findAnimation("man1", 5, 1 / 15f) }
    private val knight by lazy { findAnimation("man2", 5, 1 / 17f) }
    private val baron by lazy { findAnimation("man3", 5, 1 / 10f) }
  }

  override fun frameUpdate() {

    val currHex = islandScreen.basicInputProcessor.cursorHex

    batch.projectionMatrix = islandScreen.camera.combined

    batch.begin()

    loop@ for (hexagon in islandScreen.island.hexagons) {
      val data = hexagon.getData(islandScreen.island)
      if (data.invisible) continue

      val brightness = HexagonData.BRIGHTNESS + (if (hexagon.cubeCoordinate == currHex?.cubeCoordinate) HexagonData.SELECTED else 0f)

      batch.setColor(brightness, brightness, brightness, 1f)

      val piece = data.piece
      val drawable = when (piece) {
        is Capital -> {
          if (piece.balance < 10) capital else {
            piece.elapsedAnimationTime += Gdx.graphics.deltaTime
            capitalFlag.getKeyFrame(piece.elapsedAnimationTime)
          }
        }
        is PalmTree -> palm
        is PineTree -> pine
        is Castle -> castle
        is LivingPiece -> {
          if (piece.alive) {
            piece.updateAnimationTime()
            when (piece) {
              is Peasant -> peasant
              is Spearman -> spearman
              is Knight -> knight
              is Baron -> baron
            }.getKeyFrame(piece.elapsedAnimationTime)
          } else {
            grave
          }
        }
        is Empty -> continue@loop
      }

      val ratio = drawable.packedWidth.toFloat() / drawable.packedHeight.toFloat()

      val boundingBox = hexagon.internalBoundingBox
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
