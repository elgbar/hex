package no.elg.hex.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import ktx.graphics.use
import no.elg.hex.Hex
import no.elg.hex.Settings
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
import no.elg.hex.hexagon.strengthToTypeOrNull
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.getData
import no.elg.hex.util.requestRenderingIn

/** @author Elg */
class SpriteRenderer(private val islandScreen: PreviewIslandScreen) : FrameUpdatable, Disposable {

  private val batch: SpriteBatch = SpriteBatch()

  override fun frameUpdate() {
    val island = islandScreen.island
    var minTimeToRender = Float.MAX_VALUE
    batch.use(islandScreen.camera) {
      loop@ for (hexagon in island.visibleHexagons) {
        val data = island.getData(hexagon)

        fun shouldAnimate(): Boolean {
          return data.team == island.currentTeam && island.isCurrentTeamHuman() && !Hex.args.mapEditor
        }

        val drawable = when (val piece = data.piece) {
          is Capital -> {
            if (piece.balance >= PEASANT_PRICE && shouldAnimate()) {
              piece.elapsedAnimationTime += Gdx.graphics.deltaTime
              val capitalFlag = Hex.assets.capitalFlag
              minTimeToRender = minTimeToRender.coerceAtMost(capitalFlag.frameDuration)
              capitalFlag.getKeyFrame(piece.elapsedAnimationTime)
            } else {
              Hex.assets.capital
            }
          }

          is PalmTree -> Hex.assets.palm
          is PineTree -> Hex.assets.pine
          is Castle -> Hex.assets.castle
          is Grave -> Hex.assets.grave
          is LivingPiece -> {
            val pieceAnimation = when (piece) {
              is Peasant -> Hex.assets.peasant
              is Spearman -> Hex.assets.spearman
              is Knight -> Hex.assets.knight
              is Baron -> Hex.assets.baron
            }
            if (!piece.moved && shouldAnimate()) {
              minTimeToRender = minTimeToRender.coerceAtMost(pieceAnimation.frameDuration)
              pieceAnimation.getKeyFrame(piece.updateAnimationTime())
            } else {
              pieceAnimation.getKeyFrame(0f)
            }
          }

          is Empty -> continue@loop
        }

        val boundingBox = hexagon.internalBoundingBox
        batch.draw(
          drawable,
          boundingBox.x.toFloat(),
          boundingBox.y.toFloat(),
          boundingBox.height.toFloat(),
          boundingBox.width.toFloat()
        )
      }

      if (minTimeToRender != Float.MAX_VALUE) {
//        Gdx.app.trace("Sprite Rendering") { "[frame id ${Gdx.graphics.frameId}] Requesting frame in $minTimeToRender seconds" }
        Gdx.graphics.requestRenderingIn(minTimeToRender)
      }

      if (Settings.enableStrengthHint && island.isCurrentTeamHuman()) {
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