package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Grave
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.PEASANT_PRICE
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.Spearman
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.getData

/** @author Elg */
class SpriteRenderer(private val islandScreen: PreviewIslandScreen) : FrameUpdatable, Disposable {

  private val batch: SpriteBatch = SpriteBatch()

  override fun frameUpdate() {

    batch.use(islandScreen.camera) {

      loop@ for (hexagon in islandScreen.island.hexagons) {
        val data = islandScreen.island.getData(hexagon)
        if (data.invisible) continue

        val piece = data.piece
        val drawable =
          when (piece) {
            is Capital -> {
              if (data.team != islandScreen.island.currentTeam || piece.balance < PEASANT_PRICE) {
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

              val time = if (data.team == islandScreen.island.currentTeam && islandScreen.island.currentAI == null) piece.updateAnimationTime() else 0f

              when (piece) {
                is Peasant -> Hex.assets.peasant
                is Spearman -> Hex.assets.spearman
                is Knight -> Hex.assets.knight
                is Baron -> Hex.assets.baron
              }.getKeyFrame(time)
            }
            is Empty -> continue@loop
          }

        val boundingBox = hexagon.internalBoundingBox
        batch.draw(drawable, boundingBox.x.toFloat(), boundingBox.y.toFloat(), boundingBox.height.toFloat(), boundingBox.width.toFloat())
      }
    }
  }

  override fun dispose() {
    batch.dispose()
  }
}
