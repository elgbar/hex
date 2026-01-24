package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import no.elg.hex.Hex
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.model.FastIslandMetadata.Companion.islandMetadataFileName
import no.elg.hex.model.IslandDto
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.screens.PreviewIslandScreen
import kotlin.io.encoding.Base64

private val islandPreferences: Preferences by lazy { Gdx.app.getPreferences("island") }

fun clearIslandPreferences() {
  islandPreferences.clear()
  islandPreferences.flush()
}

private fun islandProgressKey(id: Int, preview: Boolean = false) = "$id${if (preview) "-preview" else ""}"

fun PreviewIslandScreen.saveIslandProgress() {
  Gdx.app.debug("IS PROGRESS") { "Saving progress of island ${metadata.id}" }
  island.select(null)
  if (!Hex.paused) {
    saveIslandProgress(metadata.id, island.createDto())
    Hex.assets.islandPreviews.updatePreviewFromIsland(metadata, island)
  }
}

fun saveIslandProgress(id: Int, dto: IslandDto) {
  islandPreferences.putString(islandProgressKey(id), dto.serialize())
  islandPreferences.flush()
}

fun saveIslandMetadataProgress(metadata: FastIslandMetadata) {
  islandPreferences.putString(islandMetadataFileName(metadata.id), Base64.encode(Hex.smileMapper.writeValueAsBytes(metadata)))
  islandPreferences.flush()
}

fun readIslandProgressOrNull(id: Int, allowProgressInMapEditor: Boolean): String? = readFromIslandPreferences(islandProgressKey(id), allowProgressInMapEditor)

fun readIslandMetadataProgressOrNull(id: Int, allowProgressInMapEditor: Boolean): String? = readFromIslandPreferences(islandMetadataFileName(id), allowProgressInMapEditor)

private fun readFromIslandPreferences(key: String, allowProgressInMapEditor: Boolean): String? {
  if (Hex.mapEditor && !allowProgressInMapEditor) {
    return null
  }
  return islandPreferences.getString(key, null)
}

fun clearIslandProgress(metadata: FastIslandMetadata) {
  Gdx.app.debug("IS PROGRESS") { "Clearing progress of island ${metadata.id}" }
  metadata.modifier = PreviewModifier.NOTHING
  islandPreferences.remove(islandProgressKey(metadata.id))
  islandPreferences.remove(islandMetadataFileName(metadata.id))
  islandPreferences.flush()
}