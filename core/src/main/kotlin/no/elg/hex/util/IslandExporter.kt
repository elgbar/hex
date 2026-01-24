package no.elg.hex.util

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ktx.async.KtxAsync
import no.elg.hex.Hex
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.Team
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.preview.PreviewModifier.NOTHING
import no.elg.hex.preview.PreviewModifier.WON
import no.elg.hex.screens.ImportIslandsScreen
import no.elg.hex.util.ExportedIsland.Companion.importBest
import no.elg.hex.util.FinishedExportedIslandData.Companion.DEFAULT_ROUND
import org.hexworks.mixite.core.api.CubeCoordinate
import kotlin.math.round

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@")
@JsonSubTypes(
  value = [
    JsonSubTypes.Type(value = OngoingExportedIslandData::class, name = "o"),
    JsonSubTypes.Type(value = FinishedExportedIslandData::class, name = "f")
  ]
)
sealed interface ExportedIsland {

  /**
   * Island id
   * @see FastIslandMetadata.id
   */
  val id: Int

  /**
   * Users best score for this island
   * @see FastIslandMetadata.userRoundsToBeat
   */
  val best: Int

  fun import(metadata: FastIslandMetadata): Island?

  companion object {
    fun ExportedIsland.importBest(metadata: FastIslandMetadata) {
      val metadataIsHasScore = metadata.userRoundsToBeat != Island.NEVER_PLAYED
      val exportedHasScore = best != Island.NEVER_PLAYED
      if (metadataIsHasScore && exportedHasScore && best < metadata.userRoundsToBeat) {
        // We have a better score than the current one
        metadata.userRoundsToBeat = best
      } else if (!metadataIsHasScore && exportedHasScore) {
        // Export has a score while the metadata does not
        metadata.userRoundsToBeat = best
      }
    }
  }
}

@JsonTypeName("o")
data class OngoingExportedIslandData(
  override val id: Int,
  override val best: Int,
  @JsonDeserialize(keyAs = CubeCoordinate::class, contentAs = HexagonData::class)
  val progress: Map<CubeCoordinate, HexagonData> = emptyMap()
) : ExportedIsland {

  override fun import(metadata: FastIslandMetadata): Island? {
    metadata.modifier = NOTHING
    importBest(metadata)
    return try {
      loadIslandSync(id).also { island ->
        val dto = island.createDto().withInitialData(progress)
        island.restoreState(dto)

        // Only save the island if it was successfully deserialized
        islandPreferences.putString(getPrefName(metadata.id), dto.serialize())
        islandPreferences.flush()
      }
    } catch (e: Exception) {
      MessagesRenderer.publishError("Failed to import ongoing island $id: ${e.message}")
      null
    }
  }
}

@JsonTypeName("f")
data class FinishedExportedIslandData(override val id: Int, override val best: Int, val modifier: PreviewModifier, val winningTeam: Team, val round: Int?) : ExportedIsland {
  override fun import(metadata: FastIslandMetadata): Island? {
    metadata.modifier = modifier
    importBest(metadata)
    return try {
      loadInitialIsland(id).also { island ->
        island.round = round ?: DEFAULT_ROUND
        if (modifier != PreviewModifier.SURRENDER) {
          island.fill(winningTeam)
        }
      }
    } catch (e: Exception) {
      MessagesRenderer.publishError("Failed to import finished island $id: ${e.message}")
      null
    }
  }
  companion object {
    const val DEFAULT_ROUND = 1
  }
}

fun exportIsland(metadata: FastIslandMetadata): ExportedIsland {
  val island = loadIslandSync(metadata.id)
  return if (metadata.modifier == NOTHING) {
    val progress = island.createDto().refine(loadInitialIsland(metadata.id)).hexagonData
    OngoingExportedIslandData(metadata.id, metadata.userRoundsToBeat, progress)
  } else {
    val team = when (metadata.modifier) {
      WON -> island.currentTeam
      else -> island.hexagonsPerTeam.filter { it.key != island.currentTeam }.maxByOrNull { it.value }?.key ?: Team.SUN
    }
    val round = if (island.round == DEFAULT_ROUND) null else island.round
    FinishedExportedIslandData(metadata.id, metadata.userRoundsToBeat, metadata.modifier, team, round)
  }
}

fun importIslands(exportedData: List<ExportedIsland>) {
  Hex.screen = ImportIslandsScreen(exportedData.map(::importIsland))
}

private fun importIsland(exportedData: ExportedIsland): Deferred<Pair<FastIslandMetadata, Island>?> =
  KtxAsync.async(Hex.asyncThread) {
    if (exportedData.id !in Hex.assets.islandFiles.islandIds) {
      MessagesRenderer.publishError("Unknown island with id ${exportedData.id}, will not import it")
      return@async null
    }
    val metadata = FastIslandMetadata.loadInitial(exportedData.id) ?: return@async null
    return@async exportedData.import(metadata)?.let { metadata to it }
  }