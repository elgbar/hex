package no.elg.hex.renderer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.ai.NotAsRandomAI
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.event.SimpleEventListener
import no.elg.hex.event.events.AIClickedEvent
import no.elg.hex.hexagon.Baron
import no.elg.hex.hexagon.Knight
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Peasant
import no.elg.hex.hexagon.Spearman
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.strengthToTypeOrNull
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.MIN_HEX_IN_TERRITORY
import no.elg.hex.island.Territory
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.calculateStrength
import no.elg.hex.util.getAllTerritories
import no.elg.hex.util.getData
import no.elg.hex.util.getTerritories
import no.elg.hex.util.safeUse

/** @author Elg */
class SpriteRenderer(private val islandScreen: PreviewIslandScreen) :
  FrameUpdatable,
  Disposable {

  private val batch: SpriteBatch = SpriteBatch()

  private var lastAiClicked: AIClickedEvent? = null
  private val aiClickedEvent: SimpleEventListener<AIClickedEvent> = SimpleEventListener.create<AIClickedEvent> { lastAiClicked = it }

  /**
   * AI for each team. Used to debug best castle placement.
   * Use NARI from island where possible (when the player is AI), otherwise a new instance for that team is created.
   */
  private val nari: Map<Team, NotAsRandomAI> by lazy {
    Team.entries.associateWith { islandScreen.island.teamToPlayer[it] as? NotAsRandomAI ?: NotAsRandomAI(it, 0, 1.0, 1.0) }
  }

  override fun frameUpdate() {
    val island = islandScreen.island
    batch.safeUse(islandScreen.camera) {
      loop@ for (hexagon in island.visibleHexagons) {
        val data = island.getData(hexagon)

        fun shouldAnimate(): Boolean =
          data.team == island.currentTeam &&
            island.isCurrentTeamHuman() &&
            !Hex.mapEditor &&
            !islandScreen.isPreviewRenderer &&
            islandScreen.island.gameInteraction.animate

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

      if (!islandScreen.isPreviewRenderer) {
        if ((Hex.debug || Hex.mapEditor) && Settings.enableStrengthHintEverywhere) {
          renderAllTerritoriesStrength(island)
        } else if (island.isCurrentTeamHuman()) {
          if (Hex.debug && Settings.enableStrengthHintInPlayerTerritories) {
            island.getTerritories(island.currentTeam).forEach(::renderStrengthHint)
          } else if (Settings.enableStrengthHint) {
            renderStrengthHint(island.selected)
          }
        }
        if (Hex.debug && Settings.debugAICastlePlacement) {
          island.getAllTerritories().values.flatten().forEach { territory ->
            val nari: NotAsRandomAI = nari.getValue(territory.team)
            renderBestCastlePlacement(territory, nari)
          }
        }
        if (Hex.debug && Settings.debugAIAction) {
          lastAiClicked?.also(::renderLastAiClicked)
        }
      }
    }
  }

  private fun renderAllTerritoriesStrength(island: Island) = island.getAllTerritories().values.flatten().forEach(::renderStrengthHint)

  private fun renderStrengthHint(territory: Territory?) {
    val island = territory?.island ?: return
    for (hexagon in territory.hexagons) {
      val data = island.getData(hexagon)
      val str = island.calculateStrength(hexagon, data.team)
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

  private fun renderBestCastlePlacement(territory: Territory, nari: NotAsRandomAI) {
    if (territory.hexagons.size == MIN_HEX_IN_TERRITORY) return // no point in rendering a castle placement when there is only one option (reduces visual clutter)
    val hexagon = nari.calculateBestCastlePlacement(territory) ?: return

    val boundingBox = hexagon.internalBoundingBox

    val width = boundingBox.height.toFloat() / 3
    val height = boundingBox.width.toFloat() / 3

    batch.draw(Hex.assets.castle, boundingBox.x.toFloat() + width, boundingBox.y.toFloat(), width, height)
  }

  private fun renderLastAiClicked(aiClickedEvent: AIClickedEvent) {
    val hexagon = aiClickedEvent.hexagon

    val boundingBox = hexagon.internalBoundingBox

    val width = boundingBox.height.toFloat() / 3
    val height = boundingBox.width.toFloat() / 3

    batch.color = aiClickedEvent.team.inverseColor
    batch.draw(Hex.assets.hand, boundingBox.x.toFloat(), boundingBox.y.toFloat(), width, height)
    batch.color = Color.WHITE
  }

  override fun dispose() {
    batch.dispose()
    aiClickedEvent.dispose()
  }
}