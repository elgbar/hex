package no.elg.hex.island

import com.badlogic.gdx.Gdx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ktx.assets.load
import ktx.async.KtxAsync
import ktx.async.MainDispatcher
import no.elg.hex.Hex
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.util.clearIslandProgress
import no.elg.hex.util.debug
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.info
import no.elg.hex.util.isLoaded
import no.elg.hex.util.reportTiming
import no.elg.hex.util.trace
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class IslandFiles {

  /**
   * Islands in order
   */
  val islandIds = ArrayList<Int>()

  private var _ready: AtomicBoolean = AtomicBoolean(false)
  val ready: Boolean get() = _ready.get()

  val size: Int get() = islandIds.size

  val nextIslandId: Int
    get() {
      if (islandIds.isEmpty()) return 0
      val firstExisting =
        islandIds.filterIndexed { index, i -> index + 1 != size && islandIds[index + 1] != i + 1 }
          .firstOrNull() ?: (size - 1)
      return firstExisting + 1
    }

  fun exists(islandId: Int): Boolean {
    val fileName = getIslandFileName(islandId)
    val file = getIslandFile(fileName, allowInternal = !Hex.mapEditor)
    return file.exists() && !file.isDirectory
  }

  fun fullFilesSearch() {
    runBlocking { fullFilesSearchSus() }
  }

  suspend fun fullFilesSearchSus() {
    val previewToRender = ConcurrentHashMap<Int, FastIslandMetadata>()
    _ready.set(false)
    try {
      reportTiming("do a full files search") {
        if (Hex.args.`disable-island-loading`) return
        islandIds.clear()

        var nonExistentFilesInRow = 0
        coroutineScope {
          for (id in 0..MAX_FILES_TO_SEARCH) {
            if (exists(id)) {
              if (nonExistentFilesInRow > 0) {
                Gdx.app.debug(TAG) {
                  "Missing the islands ${(id - nonExistentFilesInRow until id).map { getIslandFileName(it) }}"
                }
              }
              Gdx.app.trace(TAG) { "Found island ${getIslandFileName(id)}" }
              nonExistentFilesInRow = 0
              // add to the sync list to make the loading screen number go up
              islandIds += id

              launch(Dispatchers.Default) {
                // If there are any islands in progress, we need to load them anyway so no point in just loading initial
                val metadata = FastIslandMetadata.loadOrNull(id, false)
                if (metadata == null || metadata.forTesting && (!Hex.debug && !Hex.mapEditor)) {
                  Gdx.app.debug(TAG) { "Skipping island $id as it is for null or for debugging purposes only" }
                  FastIslandMetadata.clearInitialIslandMetadataCache(id)
                  islandIds -= id
                } else {
                  listARtBImprovements(id)
                  loadIsland(id)
                  val initalMetadaa = FastIslandMetadata.loadInitial(id)
                  if (initalMetadaa != null && initalMetadaa.revision > metadata.revision) {
                    Gdx.app.debug(TAG) {
                      "Island $id has older revision (${metadata.revision}) than initial (${initalMetadaa.revision}), clearing progress"
                    }
                    clearIslandProgress(metadata)
                    previewToRender[id] = initalMetadaa
                  } else {
                    previewToRender[id] = metadata
                  }
                }
              }
            } else {
              nonExistentFilesInRow++
              if (nonExistentFilesInRow >= FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH) {
                Gdx.app.trace(TAG) {
                  "Did not find any existing files for $FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH files after island ${id - FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH}"
                }
                break
              }
            }
          }
        }
        Gdx.app.debug(TAG) { "Next island created will be $nextIslandId" }
        if (Hex.args.listARtBImprovements) {
          Gdx.app.info(TAG) { "Done creating ARtB improvements list, exiting" }
          Gdx.app.exit()
        }
      }
    } finally {
      _ready.compareAndSet(false, true)
    }
    KtxAsync.launch(MainDispatcher) {
      Hex.assets.islandPreviews.updateAllPreviewsFromMetadata(previewToRender)
    }
  }

  private fun listARtBImprovements(id: Int) {
    if (Hex.args.listARtBImprovements) {
      val initialMetadata = FastIslandMetadata.loadInitial(id) ?: return
      val progress = FastIslandMetadata.loadProgress(id, allowProgressInMapEditor = true)
      if (progress != null) {
        progress.authorRoundsToBeat = initialMetadata.authorRoundsToBeat
        val tryUpdate = if (progress.isUserBetterThanAuthor()) {
          Gdx.app.info("ARtB") { "Island $id improved from ${progress.authorRoundsToBeat} to ${progress.userRoundsToBeat} rounds" }
          true
        } else if (progress.wasNeverBeatenByAuthorButBeatenByUser()) {
          Gdx.app.info("ARtB") { "Island $id unbeaten to ${progress.userRoundsToBeat} rounds" }
          true
        } else {
          false
        }
        if (tryUpdate && Hex.args.writeARtBImprovements) {
          initialMetadata.authorRoundsToBeat = progress.userRoundsToBeat
          Gdx.app.debug("ARtB") { "Updating island $id's ARtB to ${initialMetadata.authorRoundsToBeat} rounds" }
          initialMetadata.save()
        }
      }
    }
  }

  private fun loadIsland(id: Int) {
    if (Hex.args.`load-all-islands`) {
      val fileName = getIslandFileName(id)
      if (!Hex.assets.isLoaded<Island>(fileName)) {
        Hex.assets.load<Island>(fileName)
      }
    }
  }

  companion object {
    private const val TAG = "ISLAND FILES"

    /**
     * Stop searching for new island files when not finding this many files in a row
     */
    private const val FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH = 10
    private const val MAX_FILES_TO_SEARCH = 1000
  }
}