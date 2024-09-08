package no.elg.hex.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.island.Island
import no.elg.hex.util.regenerateCapitals
import no.elg.hex.util.reportTiming
import no.elg.hex.util.serialize

/** @author Elg */
class IslandAsynchronousAssetLoader(resolver: FileHandleResolver) :
  AsynchronousAssetLoader<Island, IslandAssetLoaderParameters>(resolver) {

  private var island: Island? = null

  override fun loadSync(manager: AssetManager, fileName: String, file: FileHandle, parameter: IslandAssetLoaderParameters?): Island? {
    return island?.also { island = null }
  }

  override fun getDependencies(fileName: String, file: FileHandle, parameter: IslandAssetLoaderParameters?) = null

  override fun loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: IslandAssetLoaderParameters?) {
    island = null
    val json: String = reportTiming("read island file as string") {
      file.readString()
    }
    island =
      reportTiming("deserialize island json") {
        Island.deserialize(json)
      }.also { island ->
        if (Hex.args.`update-saved-islands`) {
          Gdx.files.local(file.path()).writeString(
            island.also {
              it.regenerateCapitals()
              it.currentTeam = Settings.startTeam
            }.createDto().serialize(),
            false
          )
        }
      }
  }
}