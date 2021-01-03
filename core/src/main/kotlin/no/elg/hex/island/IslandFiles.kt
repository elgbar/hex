package no.elg.hex.island

import com.badlogic.gdx.Gdx
import no.elg.hex.Hex
import no.elg.hex.util.getIslandFile
import no.elg.hex.util.getIslandFileName
import no.elg.hex.util.trace

object IslandFiles {

  /**
   * Stop searching for new island files when not finding this many files in a row
   */
  private const val FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH = 500
  private const val TAG = "ISLAND FILES"

  /**
   * Islands in order
   */
  val islandIds = ArrayList<Int>()

  init {
    fullFilesSearch()
  }

  val nextIslandId: Int
    get() {
      if (islandIds.isEmpty()) return 0
      val firstExisting =
        islandIds.filterIndexed { index, i -> index + 1 != islandIds.size && islandIds[index + 1] != i + 1 }.firstOrNull() ?: (islandIds.size - 1)
      return firstExisting + 1
    }

  fun fullFilesSearch() {
    if (Hex.args.`disable-island-loading`) return
    islandIds.clear()

    var nonExistentFilesInRow = 0
    for (slot in 0..Int.MAX_VALUE) {
      val file = getIslandFile(slot, allowInternal = !Hex.args.mapEditor)
      if (file.exists()) {
        if (file.isDirectory) continue
        if (nonExistentFilesInRow > 0) {
          Gdx.app.debug(TAG, "Missing the islands ${(slot - nonExistentFilesInRow until slot).map { getIslandFileName(it) }}")
        }
        val fileName = getIslandFileName(slot)
        Gdx.app.trace(TAG, "Found island $fileName")
        nonExistentFilesInRow = 0

        if (Hex.args.`load-all-islands` && !Hex.assets.isLoaded(fileName, Island::class.java)) {
          Hex.assets.load(fileName, Island::class.java)
        }
        islandIds += slot
      } else {

        nonExistentFilesInRow++
        if (nonExistentFilesInRow >= FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH) {
          Gdx.app.trace(
            TAG,
            "Did not find any existing files for $FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH files " +
              "after island ${slot - FILE_NOT_FOUND_IN_ROW_TO_STOP_SEARCH}"
          )
          break
        }
      }
    }
    Gdx.app.debug(TAG, "Next island created will be $nextIslandId")
  }
}
