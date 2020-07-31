package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import no.elg.hex.Hex
import no.elg.island.Island
import org.hexworks.mixite.core.api.HexagonalGridLayout.RECTANGULAR

/**
 * @author Elg
 */
object LevelSelectScreen : AbstractScreen() {

  private val islandPreviews = ArrayList<FrameBuffer>()

  override fun onLoad() {

    val file = Gdx.files.local(ISLAND_SAVE_DIR)


    play(0)
  }

  override fun render(delta: Float) {

  }


  private const val ISLAND_SAVE_DIR = "islands"

  fun getIslandFile(slot: Int): FileHandle = Gdx.files.local("$ISLAND_SAVE_DIR/island-$slot.is")


  fun play(slot: Int) {
    play(getIslandFile(slot))
  }

  fun play(file: FileHandle) {
    val island = loadIsland(file) ?: Island(40, 25, RECTANGULAR)
    play(island)
  }

  fun play(island: Island) {
    Gdx.app.postRunnable { Hex.screen = IslandScreen(island) }
  }

  fun loadIsland(file: FileHandle): Island? {
    val json: String = try {
      requireNotNull(file.readString())
    } catch (e: Exception) {
      Gdx.app.log("LOAD", "Failed to load island the name '${file.name()}'")
      return null
    }

    return try {
      val island = Island.deserialize(json)
      Gdx.app.log("LOAD", "Successfully loaded island '${file.name()}'")
      island
    } catch (e: Exception) {
      Gdx.app.log("LOAD", "Invalid island save data for island '${file.name()}'")
      Gdx.app.log("LOAD", e.message)
      null
    }
  }


}
