package no.elg.hex.renderer

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.Spearman
import no.elg.hex.hexagon.strengthToTypeOrNull
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.getData

/** @author Elg */
class SpriteRenderer(private val islandScreen: PreviewIslandScreen) : FrameUpdatable, Disposable {

  private val batch: SpriteBatch = SpriteBatch()

  private val isNotPreview = !Hex.args.mapEditor && !islandScreen.isPreviewRenderer

  override fun frameUpdate() {
    val island = islandScreen.island
    batch.use(islandScreen.camera) {
      loop@ for (hexagon in island.visibleHexagons) {
        val data = island.getData(hexagon)

        fun shouldAnimate(): Boolean {
          return data.team == island.currentTeam && island.isCurrentTeamHuman() && isNotPreview
        }

        val drawable = Hex.assets.getTexture(data.piece, shouldAnimate()) ?: continue@loop

        val boundingBox = hexagon.internalBoundingBox
        batch.draw(
          drawable,
          boundingBox.x.toFloat(),
          boundingBox.y.toFloat(),
          boundingBox.height.toFloat(),
          boundingBox.width.toFloat()
        )
      }

      if (isNotPreview && Settings.enableStrengthHint && island.isCurrentTeamHuman()) {
        val territory = island.selected
        if (territory != null) {
          for (hexagon in territory.hexagons) {
            val data = island.getData(hexagon)
            val str = island.calculateStrength(hexagon)
            // Only show strength hint if the piece is different from the current piece
            // i.e., don't show a spearman as a hint if the current piece is a spearman
            if (data.piece is LivingPiece && str <= data.piece.strength) continue
            val typ = strengthToTypeOrNull(str) ?: continue

            val drawable = when (typ) {
              Peasant::class -> Hex.assets.peasant
              Spearman::class -> Hex.assets.spearman
              Knight::class -> Hex.assets.knight
              Baron::class -> Hex.assets.baron
              else -> continue
            }.getKeyFrame(0f)

            val boundingBox = hexagon.internalBoundingBox

            val width = boundingBox.height.toFloat() / 3
            val height = boundingBox.width.toFloat() / 3
            batch.draw(drawable, boundingBox.x.toFloat(), boundingBox.y.toFloat(), width, height)
          }
        }
      }
    }
  }

  override fun dispose() {
    batch.dispose()
  }
}