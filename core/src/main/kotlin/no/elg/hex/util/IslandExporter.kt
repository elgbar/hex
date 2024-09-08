package no.elg.hex.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ktx.async.KtxAsync
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.screens.ImportIslandsScreen

data class ExportedIslandData(val id: Int, val modifier: PreviewModifier?, val islandProgress: String?, val round: Int? = null)

fun exportIsland(metadata: FastIslandMetadata): ExportedIslandData {
  val progress = getIslandProgress(metadata.id)
  val modifier = if (metadata.modifier == PreviewModifier.NOTHING) null else metadata.modifier
  return ExportedIslandData(metadata.id, modifier, progress)
}

fun importIslands(exportedData: List<ExportedIslandData>) {
  val importJobs = exportedData.map(::importIsland)
  Hex.screen = ImportIslandsScreen(importJobs)
}

private fun importIsland(exportedData: ExportedIslandData): Deferred<Pair<FastIslandMetadata, Island>?> =
  KtxAsync.async(Hex.asyncThread) {
    if (exportedData.id !in Hex.assets.islandFiles.islandIds) {
      MessagesRenderer.publishError("Unknown island with id ${exportedData.id}, will not import it")
      return@async null
    }
    val metadata = FastIslandMetadata.loadProgress(exportedData.id) ?: return@async null
    metadata.modifier = exportedData.modifier ?: PreviewModifier.NOTHING
    val island = exportedData.islandProgress?.let { progress ->
      try {
        Island.deserialize(progress).also {
          // Only save the island if it was successfully deserialized
          islandPreferences.putString(getPrefName(metadata.id), progress)
          islandPreferences.flush()
        }
      } catch (e: Exception) {
        MessagesRenderer.publishError("Failed to import island ${exportedData.id}")
        return@async null
      }
    } ?: return@async null
    return@async metadata to island
  }