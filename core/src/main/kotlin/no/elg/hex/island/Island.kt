package no.elg.hex.island

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Hex
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.input.BasicInputHandler
import no.elg.hex.util.calculateRing
import no.elg.hex.util.connectedHexagons
import no.elg.hex.util.getData
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonOrientation.FLAT_TOP
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout
import kotlin.math.max


/**
 * @author Elg
 */
class Island(
  width: Int,
  height: Int,
  layout: HexagonalGridLayout,
  hexagonData: Set<Pair<CubeCoordinate, HexagonData>> = emptySet()
) {

  val grid: HexagonalGrid<HexagonData>

  /**
   * Prefer this over calling [grid.hexagons] as this has better performance
   */
  val hexagons: Set<Hexagon<HexagonData>>

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
    hexagons = grid.hexagons.toSet()

    Gdx.app.postRunnable {
      for (hexagon in hexagons) {
        select(hexagon)
      }
    }

  }

  var selected: Territory? = null
    private set

  //////////////
  // Gameplay //
  //////////////


  /**
   * Select the hex under the cursor
   */
  fun select(hex: Hexagon<HexagonData>? = BasicInputHandler.cursorHex) {
    selected = null
    if (hex == null) return

    val data: HexagonData = hex.getData()

    val territoryHexes = hex.getTerritoryHexagons() ?: return

    val capital = getCapital(territoryHexes).let {
      if (it != null) it
      else {
        val capHex = calculateBestCapitalPlacement(territoryHexes).getData()
        capHex.setPiece(Capital::class)
        capHex.piece as Capital
      }
    }
    selected = Territory(capital, territoryHexes)
  }

  fun Hexagon<HexagonData>.getCapital(): Capital? {
    val territoryHexagons = this.getTerritoryHexagons() ?: return null
    return getCapital(territoryHexagons)
  }

  private fun getCapital(hexagons: Set<Hexagon<HexagonData>>): Capital? {
    return hexagons.firstOrNull { it.getData().piece is Capital }?.getData()?.piece as? Capital?
  }

  /**
   * Get all hexagons that is in tha same territory as the given [this@getTerritoryHexagons]. or null if hexagon is not a part of a territory
   */
  fun Hexagon<HexagonData>.getTerritoryHexagons(): Set<Hexagon<HexagonData>>? {
    val territoryHexes = connectedHexagons()
    if (territoryHexes.size < MIN_HEX_IN_TERRITORY) return null
    return territoryHexes
  }

  /**
   * Find the hexagon where the next capital should be placed. It should be the hex that
   *
   * 1. Is the furthest away from hexagons of other teams
   * 2. Has the most hexagons with the name team around it.
   *
   * The first point has priority over the second.
   *
   * Edge hexagons count as team hexagons
   *
   * @param hexagons All hexagons in a territory, must have a size equal to or greater than [MIN_HEX_IN_TERRITORY]
   */
  fun calculateBestCapitalPlacement(hexagons: Set<Hexagon<HexagonData>>): Hexagon<HexagonData> {
    require(hexagons.size >= MIN_HEX_IN_TERRITORY) { "There must be at least $MIN_HEX_IN_TERRITORY hexagons in the given set!" }
    val hexTeam = hexagons.first().getData().team
    require(hexagons.all { it.getData().team == hexTeam }) { "All hexagons given must be on the same team" }

    //TODO make sure to take into account if there are already any pieces there
//    hexagons.map { it.getData().piece.capitalPlacement to }

    val contenders = HashSet<Hexagon<HexagonData>>(hexagons.size)

    //The maximum distance between two hexagons for this grid
    val maxRadius = 3 * max(grid.gridData.gridWidth, grid.gridData.gridHeight) + 1

    var greatestDistance = 1


    fun finDistanceToClosestHex(hex: Hexagon<HexagonData>, discardIfLessThan: Int): Int {
      for (r in discardIfLessThan..maxRadius) {
        if (hex.calculateRing(r).any { it.getData().team != hexTeam }) {
          return r
        }
      }
      return -1 //no hexes found we've won!
    }

    for (hex in hexagons) {
      val dist = finDistanceToClosestHex(hex, greatestDistance)
      if (dist > greatestDistance) {
        //we have a new greatest distance
        greatestDistance = dist
        contenders.clear()
      }
      contenders += hex
    }

    require(contenders.isNotEmpty()) { "No capital contenders found!" }

    Gdx.app.debug("ISLAND", "There are ${contenders.size} hexes to become capital. Each of them have a minimum radius to other hexagons of $greatestDistance")

    if (contenders.size == 1) return contenders.first()

    //if we have multiple contenders to become the capital, select the one with fewest enemy hexagons near it
    // invisible hexagons count to ours hexagons

    //number of hexagons expected to have around the given radius
    val expectedHexagons = 6 * greatestDistance

    return contenders.map { origin: Hexagon<HexagonData> ->
      val ring = origin.calculateRing(greatestDistance)
      origin to ((expectedHexagons - ring.size) //non-existent hexes count as ours
        + ring.sumByDouble {
        val data = it.getData()
        (if (data.team == hexTeam) 1.0 else 0.0) + (if (data.invisible) 0.5 else 0.0)
      })
    }.maxBy { it.second }!!.first
  }

  ///////////////////
  // Serialization //
  ///////////////////

  fun serialize(): String =
    Hex.mapper.writeValueAsString(Hex.island)

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


  companion object {
    const val GRID_RADIUS = 20.0

    const val MIN_HEX_IN_TERRITORY = 2

    fun deserialize(json: String): Island {
      return Hex.mapper.readValue(json)
    }

    const val ISLAND_SAVE_DIR = "islands"

    val fileName: String get() = "island-${BasicInputHandler.saveSlot}.is"
    val islandFile: FileHandle get() = Gdx.files.local("$ISLAND_SAVE_DIR/$fileName")

    fun loadIsland(file: FileHandle = islandFile): Boolean {
      val json: String = try {
        requireNotNull(file.readString())
      } catch (e: Exception) {
        Gdx.app.log("LOAD", "Failed to load island the name '${file.name()}'")
        return false
      }
      Hex.island = try {
        deserialize(json)
      } catch (e: Exception) {
        Gdx.app.log("LOAD", "Invalid island save data for island '${file.name()}'")
        Gdx.app.log("LOAD", e.message)
        return false
      }

      Gdx.app.log("LOAD", "Successfully loaded island '${file.name()}'")
      return true
    }
  }

  //////////////////////////
  // Data Transfer Object //
  //////////////////////////

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
