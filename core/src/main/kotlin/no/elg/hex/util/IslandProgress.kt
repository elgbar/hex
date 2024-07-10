package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import no.elg.hex.Hex
import no.elg.hex.screens.PreviewIslandScreen

val islandPreferences: Preferences by lazy { Gdx.app.getPreferences("island") }
fun getPrefName(id: Int, preview: Boolean) = "$id${if (preview) "-preview" else ""}"
fun getProgress(id: Int, preview: Boolean = false): String? {
  if (Hex.args.mapEditor) {
    return null
  }
  val pref = getPrefName(id, preview)
  return islandPreferences.getString(pref, null)
}

fun PreviewIslandScreen.saveProgress() {
  Gdx.app.debug("IS PROGRESS", "Saving progress of island ${metadata.id}")
  island.select(null)
  islandPreferences.putString(getPrefName(metadata.id, false), island.createDto().serialize())
  islandPreferences.flush()
  if (!Hex.paused) {
    Hex.assets.islandPreviews.updateSelectPreview(metadata, island)
  }
}

fun clearIslandProgress(id: Int) {
  Gdx.app.debug("IS PROGRESS", "Clearing progress of island $id")
  islandPreferences.remove(getPrefName(id, false))
  islandPreferences.remove(getPrefName(id, true))
  islandPreferences.flush()
}