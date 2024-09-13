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
import no.elg.hex.screens.ImportIslandsScreen
import org.hexworks.mixite.core.api.CubeCoordinate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
  value = [
    JsonSubTypes.Type(value = OngoingExportedIslandData::class, name = "o"),
    JsonSubTypes.Type(value = FinishedExportedIslandData::class, name = "f")
  ]
)
sealed interface ExportedIsland {

  val id: Int

  fun import(metadata: FastIslandMetadata): Island?
}

@JsonTypeName("o")
data class OngoingExportedIslandData(
  override val id: Int,
  @JsonDeserialize(keyAs = CubeCoordinate::class, contentAs = HexagonData::class)
  val progress: Map<CubeCoordinate, HexagonData> = emptyMap()
) : ExportedIsland {

  override fun import(metadata: FastIslandMetadata): Island? {
    metadata.modifier = PreviewModifier.NOTHING
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
data class FinishedExportedIslandData(override val id: Int, val modifier: PreviewModifier, val winningTeam: Team) : ExportedIsland {
  override fun import(metadata: FastIslandMetadata): Island? {
    metadata.modifier = modifier
    metadata.winningTeam = winningTeam
    return try {
      loadInitialIsland(id).apply {
        fill(winningTeam)
      }
    } catch (e: Exception) {
      MessagesRenderer.publishError("Failed to import finished island $id: ${e.message}")
      null
    }
  }
}

fun exportIsland(metadata: FastIslandMetadata): ExportedIsland {
  return if (metadata.modifier == PreviewModifier.NOTHING) {
    val progress = loadIslandSync(metadata.id).createDto().refine(loadInitialIsland(metadata.id)).hexagonData
    OngoingExportedIslandData(metadata.id, progress)
  } else {
    FinishedExportedIslandData(metadata.id, metadata.modifier, metadata.winningTeam ?: Team.LEAF)
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