package no.elg.hex.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.preview.PreviewModifier

data class ExportedIslandData(val id: Int, val modifier: PreviewModifier?, val islandProgress: String?, val round: Int? = null)

fun exportIsland(metadata: FastIslandMetadata): ExportedIslandData {
  val progress = getIslandProgress(metadata.id)
  val modifier = if (metadata.modifier == PreviewModifier.NOTHING) null else metadata.modifier
  return ExportedIslandData(metadata.id, modifier, progress)
}

suspend fun importIslands(exportedData: List<ExportedIslandData>) {
  MessagesRenderer.publishMessage("Starting to import ${exportedData.size} islands from clipboard")
  val importJobs = exportedData.mapNotNull(::importIsland)
  importJobs.joinAll()
  Hex.assets.islandPreviews.sortIslands()
  MessagesRenderer.publishMessage("Imported ${importJobs.size} islands from clipboard")
  playMoney()
}

private fun importIsland(exportedData: ExportedIslandData): Job? {
  if (exportedData.id !in Hex.assets.islandFiles.islandIds) {
    MessagesRenderer.publishError("Unknown island with id ${exportedData.id}, will not import it")
    return null
  }
  val metadata = FastIslandMetadata.load(exportedData.id)
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
      return null
    }
  }
  return Hex.assets.islandPreviews.updateSelectPreview(metadata, island)
}