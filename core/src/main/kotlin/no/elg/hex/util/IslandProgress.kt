package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import no.elg.hex.Hex
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.preview.PreviewModifier
import no.elg.hex.screens.PreviewIslandScreen

val islandPreferences: Preferences by lazy { Gdx.app.getPreferences("island") }
fun getPrefName(id: Int, preview: Boolean = false) = "$id${if (preview) "-preview" else ""}"
fun getIslandProgress(id: Int, preview: Boolean = false): String? {
  if (Hex.mapEditor) {
    return null
  }
  val pref = getPrefName(id, preview)
  return islandPreferences.getString(pref, null)
}

fun PreviewIslandScreen.saveIslandProgress() {
  Gdx.app.debug("IS PROGRESS", "Saving progress of island ${metadata.id}")
  island.select(null)
  islandPreferences.putString(getPrefName(metadata.id), island.createDto().serialize())
  islandPreferences.flush()
  if (!Hex.paused) {
    Hex.assets.islandPreviews.updateSelectPreview(metadata, island)
  }
}

fun clearIslandProgress(metadata: FastIslandMetadata) {
  Gdx.app.debug("IS PROGRESS", "Clearing progress of island ${metadata.id}")
  metadata.modifier = PreviewModifier.NOTHING
  islandPreferences.remove(getPrefName(metadata.id))
  islandPreferences.remove(getPrefName(metadata.id, true)) // in case the preview is very old
  islandPreferences.flush()
}