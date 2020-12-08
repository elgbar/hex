package no.elg.hex.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.hud.ScreenText
import no.elg.hex.island.Island
import no.elg.hex.util.regenerateCapitals
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
    fileName: String,
    file: FileHandle,
    parameter: IslandAssetLoaderParameters?
  ) {
    island = null
    val json: String =
      try {
        requireNotNull(file.readString())
      } catch (e: Exception) {
        MessagesRenderer.publishMessage(
          ScreenText("Failed to load island the name '${file.name()}'", color = Color.RED)
        )
        "invalid island json"
      }
    island =
      try {
        Island.deserialize(json)
      } catch (e: Exception) {
        MessagesRenderer.publishMessage(ScreenText("Invalid island save data for island '${file.name()}'", color = Color.RED))
        Gdx.app.debug("LOAD", e.message)
        Island(25, 25, HEXAGONAL).also { it.regenerateCapitals() }
      }
  }
}
