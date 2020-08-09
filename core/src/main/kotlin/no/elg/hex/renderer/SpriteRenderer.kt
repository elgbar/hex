package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Grave
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.Spearman
import no.elg.hex.island.Island.Companion.PLAYER_TEAM
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getData

/**
 * @author Elg
 */
class SpriteRenderer(private val islandScreen: IslandScreen) : FrameUpdatable, Disposable {


  private val batch: SpriteBatch = SpriteBatch()

  override fun frameUpdate() {
    val currHex = islandScreen.basicInputProcessor.cursorHex
    batch.projectionMatrix = islandScreen.camera.combined
    batch.begin()

    loop@ for (hexagon in islandScreen.island.hexagons) {
      val data = islandScreen.island.getData(hexagon)
      if (data.invisible) continue

      val brightness = HexagonData.BRIGHTNESS + (if (hexagon.cubeCoordinate == currHex?.cubeCoordinate) HexagonData.SELECTED else 0f)

      batch.setColor(brightness, brightness, brightness, 1f)

      val piece = data.piece
      val drawable = when (piece) {
        is Capital -> {
          if (data.team != PLAYER_TEAM || piece.balance < 10) {
            Hex.assets.capital
          } else {
            piece.elapsedAnimationTime += Gdx.graphics.deltaTime
            Hex.assets.capitalFlag.getKeyFrame(piece.elapsedAnimationTime)
          }
        }
        is PalmTree -> Hex.assets.palm
        is PineTree -> Hex.assets.pine
        is Castle -> Hex.assets.castle
        is Grave -> Hex.assets.grave
        is LivingPiece -> {

          val time = if (data.team == PLAYER_TEAM) piece.updateAnimationTime() else 0f
          when (piece) {
            is Peasant -> Hex.assets.peasant
            is Spearman -> Hex.assets.spearman
            is Knight -> Hex.assets.knight
            is Baron -> Hex.assets.baron
          }.getKeyFrame(time)
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
