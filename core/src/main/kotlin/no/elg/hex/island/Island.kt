package no.elg.hex.island

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Hex
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.input.MapEditorInput
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.HexagonOrientation.FLAT_TOP
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout


/**
 * @author Elg
 */
class Island(width: Int, height: Int, layout: HexagonalGridLayout, hexagonData: Set<Pair<CubeCoordinate, HexagonData>> = emptySet()) {

  val grid: HexagonalGrid<HexagonData>

  companion object {
    const val GRID_RADIUS = 20.0

    fun deserialize(json: String): Island {
      return Hex.mapper.readValue(json)
    }

    const val ISLAND_SAVE_DIR = "islands"

    val fileName: String get() = "island-${MapEditorInput.saveSlot}.is"
    val islandFile: FileHandle get() = Gdx.files.local("$ISLAND_SAVE_DIR/$fileName")

    fun loadIsland(): Boolean {
      val name = fileName
      val file = islandFile
      val json: String = try {
        requireNotNull(file.readString())
      } catch (e: Exception) {
        Gdx.app.log("LOAD", "Failed to load island the name '$name'")
        return false
      }
      Hex.island = try {
        deserialize(json)
      } catch (e: Exception) {
        Gdx.app.log("LOAD", "Invalid island save data for island '$name'")
        return false
      }

      Gdx.app.log("LOAD", "Successfully loaded island '$name'")
      return true
    }
  }

  init {
    val builder = HexagonalGridBuilder<HexagonData>()
      .setGridWidth(width)
      .setGridHeight(height)
      .setGridLayout(layout)
      .setOrientation(FLAT_TOP)
      .setRadius(GRID_RADIUS)

    grid = builder.build()

    if (hexagonData.isNotEmpty()) {
      for ((coord, data) in hexagonData) {
        grid.getByCubeCoordinate(coord).ifPresent {
          it.setSatelliteData(data)
        }
      }
    }
  }


  val hexagons = grid.hexagons.toSet()

  fun serialize(): String = Hex.mapper.writeValueAsString(Hex.island)

  fun saveIsland(): Boolean {
    val name = fileName
    val file = islandFile

    if (file.isDirectory) {
      Gdx.app.log("SAVE", "Failed to save island the name '$name' as the resulting file will be a directory.")
      return false
    }
    file.writeString(serialize(), false)
    Gdx.app.log("SAVE", "Successfully saved island '$name'")
    return true
  }

  @get:JsonValue
  private val dto
    get() =
      IslandDTO(
        grid.gridData.gridWidth,
        grid.gridData.gridHeight,
        grid.gridData.gridLayout,
        grid.hexagons.mapTo(HashSet()) { it.cubeCoordinate to it.getData() }
      )

  private data class IslandDTO(val width: Int, val height: Int, val layout: HexagonalGridLayout, val hexagonData: Set<Pair<CubeCoordinate, HexagonData>>)
}
