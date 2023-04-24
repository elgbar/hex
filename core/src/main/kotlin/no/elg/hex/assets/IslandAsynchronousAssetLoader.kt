package no.elg.hex.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import no.elg.hex.Hex
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.island.Island
import no.elg.hex.util.regenerateCapitals
import no.elg.hex.util.reportTiming
import no.elg.hex.util.serialize
import org.hexworks.mixite.core.api.HexagonalGridLayout.HEXAGONAL

/** @author Elg */
class IslandAsynchronousAssetLoader(resolver: FileHandleResolver) :
  AsynchronousAssetLoader<Island, IslandAssetLoaderParameters>(resolver) {

  private var island: Island? = null

  override fun loadSync(
    manager: AssetManager,
    fileName: String,
    file: FileHandle,
    parameter: IslandAssetLoaderParameters?
  ): Island? {
    return island?.also { island = null }
  }

  override fun getDependencies(
    fileName: String,
    file: FileHandle,
    parameter: IslandAssetLoaderParameters?
  ) = null

  override fun loadAsync(
    manager: AssetManager,
    fileNameOrJson: String,
    file: FileHandle,
    parameter: IslandAssetLoaderParameters?
  ) {
    island = null
    val json: String = if (parameter?.fileNameIsIsland == true) {
      fileNameOrJson
    } else {
      try {
        reportTiming("read island file as string") {
          file.readString()
        }
      } catch (e: Exception) {
        MessagesRenderer.publishError("Failed to load island the name '${file.name()}'")
        "invalid island json"
      }
    }
    island = try {
      val island = reportTiming("deserialize island json") {
        Island.deserialize(json)
      }
      if (Hex.args.`save-island-on-loading-it`) {
        Gdx.files.local(file.path()).writeString(island.createDto().serialize(), false)
      }
      island
    } catch (e: Exception) {
      MessagesRenderer.publishError("Invalid island save data for island '${file.name()}'")
      Gdx.app.log("ISLAND LOADING", e.message)
      Island(25, 25, HEXAGONAL).also { it.regenerateCapitals() }
    }
  }
}