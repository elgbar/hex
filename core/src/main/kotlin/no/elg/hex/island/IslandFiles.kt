package no.elg.hex.island

import com.badlogic.gdx.Gdx
import ktx.assets.load
import no.elg.hex.Hex
import no.elg.hex.model.FastIslandMetadata
import no.elg.hex.util.debug
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.info
import no.elg.hex.util.isLoaded
import no.elg.hex.util.reportTiming
import no.elg.hex.util.trace

class IslandFiles {

  /**
   * Islands in order
   */
  val islandIds = ArrayList<Int>()

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
    reportTiming("do a full files search") {
      if (Hex.args.`disable-island-loading`) return
      islandIds.clear()

      var nonExistentFilesInRow = 0
      for (id in 0..Int.MAX_VALUE) {
        if (exists(id)) {
          if (nonExistentFilesInRow > 0) {
            Gdx.app.debug(TAG) {
              "Missing the islands ${(id - nonExistentFilesInRow until id).map { getIslandFileName(it) }}"
            }
          }
          Gdx.app.trace(TAG) { "Found island ${getIslandFileName(id)}" }
          nonExistentFilesInRow = 0

          val initialMetadata = FastIslandMetadata.loadInitial(id)
          if (initialMetadata?.forTesting == true && (!Hex.debug && !Hex.mapEditor)) {
            Gdx.app.debug(TAG) { "Skipping island $id as it is for debugging purposes only" }
            FastIslandMetadata.clearInitialIslandMetadataCache(id)
            continue
          }

          islandIds += id

          if (initialMetadata != null && Hex.args.listARtBImprovements) {
            val progress = FastIslandMetadata.loadProgress(id)
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
          if (Hex.args.`load-all-islands`) {
            val fileName = getIslandFileName(id)
            if (!Hex.assets.isLoaded<Island>(fileName)) {
              Hex.assets.load<Island>(fileName)
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
      Gdx.app.debug(TAG, "Next island created will be $nextIslandId")
      if (Hex.args.listARtBImprovements) {
        Gdx.app.info(TAG) { "Done creating ARtB improvements list, exiting" }
        Gdx.app.exit()
      }
    }
  }

  companion object {
    private const val TAG = "ISLAND FILES"

    /**
     * Stop searching for new island files when not finding this many files in a row
     */
    private const val FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH = 10
  }
}