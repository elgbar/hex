package no.elg.hex.island

import com.badlogic.gdx.Gdx
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.elg.hex.Hex
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.TreePiece
import no.elg.hex.util.calculateRing
import no.elg.hex.util.connectedHexagons
import no.elg.hex.util.getData
import no.elg.hex.util.next
import no.elg.hex.util.trace
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
  hexagonData: Map<CubeCoordinate, HexagonData> = emptyMap()
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

    for (hexagon in hexagons) {
      val data = this.getData(hexagon)
      require(data.piece == Empty || data == data.piece.data) {
        "Found a mismatch between the piece team and the hexagon data team! FML coords ${hexagon.cubeCoordinate.toAxialKey()}"
      }
    }
    for (hexagon in hexagons) {
      val piece = this.getData(hexagon).piece
      when (piece) {
        is Capital -> piece.balance = START_CAPITAL
        is LivingPiece -> piece.moved = false
        is TreePiece -> piece.hasGrown = false
        else -> {
          /* NOP */
        }
      }
    }

  }

  var selected: Territory? = null
    private set

  var inHand: Hand? = null
    set(value) {
      field?.apply {
        if (holding) {
          if (piece.data === HexagonData.EDGE_DATA) {
            territory.capital.balance += piece.price
          } else if (piece.data.setPiece(piece::class)) {
            val newPiece = piece.data.piece
            if (newPiece is LivingPiece) {
              newPiece.moved = false
            }
          }
        }
      }
      field = value
    }

  private var currentTeam: Team = PLAYER_TEAM

  //////////////
  // Gameplay //
  //////////////

  fun endTurn() {
    select(null)
    do {
      Gdx.app.debug("TURN", "Ending turn of $currentTeam")
      for (hexagon in hexagons) {
        val data = this.getData(hexagon)
        if (data.team != currentTeam) continue
        data.piece.endTurn(this, hexagon, data, currentTeam)
      }
      currentTeam = Team.values().next(currentTeam)
    } while (currentTeam != PLAYER_TEAM)

    for (hexagon in hexagons) {
      this.getData(hexagon).piece.newRound(this, hexagon)
    }
  }

  /**
   * Select the hex under the cursor
   */
  fun select(hex: Hexagon<HexagonData>?) {

    inHand = null

    selected = null
    if (hex == null) return

    val territoryHexes = getTerritoryHexagons(hex) ?: return
    val team = this.getData(territoryHexes.first()).team
    require(territoryHexes.all { this.getData(it).team == team }) { "Wrong team!" }

    val capital = getCapitalOf(territoryHexes).let {
      if (it != null) return@let it
      else {
        val capHexData = this.getData(calculateBestCapitalPlacement(territoryHexes))
        require(capHexData.team == team) { "Cap wrong team when creating" }
        return@let if (capHexData.setPiece(Capital::class)) capHexData.piece as Capital else null
      }
    }

    if (capital != null) {
      selected = Territory(this, capital, territoryHexes)
    }
  }

  fun getCapitalOf(hexagons: Collection<Hexagon<HexagonData>>): Capital? {
    val capitals = hexagons.filter { this.getData(it).piece is Capital }
    if (capitals.isEmpty()) return null
    else if (capitals.size == 1) return this.getData(capitals.first()).piece as Capital

    //there might be multiple capitals in the set of hexagons. Find the best one, transfer all assets and delete the others

    val bestCapitalHex = calculateBestCapitalPlacement(capitals)
    val bestData = this.getData(bestCapitalHex).piece as Capital
    for (capital in capitals) {
      if (capital === bestCapitalHex) continue

      val data = this.getData(capital)
      val otherCapital = data.piece as Capital
      otherCapital.transfer(bestData)
      data.setPiece(Empty::class)
    }
    return bestData
  }

  /**
   * Get all hexagons that is in tha same territory as the given [this@getTerritoryHexagons]. or null if hexagon is not a part of a territory
   */
  fun getTerritoryHexagons(hexagon: Hexagon<HexagonData>): Set<Hexagon<HexagonData>>? {
    val territoryHexes = connectedHexagons(hexagon)
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
  fun calculateBestCapitalPlacement(hexagons: Collection<Hexagon<HexagonData>>): Hexagon<HexagonData> {
    require(hexagons.size >= MIN_HEX_IN_TERRITORY) { "There must be at least $MIN_HEX_IN_TERRITORY hexagons in the given set!" }
    val hexTeam = this.getData(hexagons.first()).team
    require(hexagons.all { this.getData(it).team == hexTeam }) { "All hexagons given must be on the same team" }

    //TODO make sure to take into account if there are already any pieces there
//    hexagons.map { it.getData(this).piece.capitalPlacement to }

    val contenders = HashSet<Hexagon<HexagonData>>(hexagons.size)

    //The maximum distance between two hexagons for this grid
    val maxRadius = 3 * max(grid.gridData.gridWidth, grid.gridData.gridHeight) + 1

    var greatestDistance = 1


    fun finDistanceToClosestHex(hex: Hexagon<HexagonData>, discardIfLessThan: Int): Int {
      for (r in discardIfLessThan..maxRadius) {
        if (this.calculateRing(hex, r).any { this.getData(it).team != hexTeam }) {
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

    Gdx.app.trace("ISLAND", "There are ${contenders.size} hexes to become capital. Each of them have a minimum radius to other hexagons of $greatestDistance")

    if (contenders.size == 1) return contenders.first()

    //if we have multiple contenders to become the capital, select the one with fewest enemy hexagons near it
    // invisible hexagons count to ours hexagons

    //number of hexagons expected to have around the given radius
    val expectedHexagons = 6 * greatestDistance

    return contenders.map { origin: Hexagon<HexagonData> ->
      val ring = this.calculateRing(origin, greatestDistance)
      origin to ((expectedHexagons - ring.size) //non-existent hexes count as ours
        + ring.sumByDouble {
        val data = this.getData(it)
        (if (data.team == hexTeam) 1.0 else 0.0) + (if (data.invisible) 0.5 else 0.0)
      })
    }.maxBy { it.second }!!.first
  }

  ///////////////////
  // Serialization //
  ///////////////////

  /**
   *
   * Validation rules:
   *
   * * All visible hexagons must be reachable from all other visible hexagons (ie there can only be one island)
   * * No capital pieces in territories with size smaller than [MIN_HEX_IN_TERRITORY]
   * * There must be exactly one capital per territory
   *
   * @return If this island is valid.
   */
  fun validate(): Boolean {
    var valid = true

    val checkedHexagons = HashSet<Hexagon<HexagonData>>()

    for (hexagon in hexagons) {
      if (checkedHexagons.contains(hexagon) || this.getData(hexagon).invisible) continue

      val connectedHexes = this.connectedHexagons(hexagon)
      checkedHexagons.addAll(connectedHexes)

      if (connectedHexes.size < MIN_HEX_IN_TERRITORY) {
        if (this.getData(hexagon).piece is Capital) {
          Gdx.app.log("Island Validation", "Hexagon ${hexagon.cubeCoordinate.toAxialKey()} is a capital, even though it has fewer than $MIN_HEX_IN_TERRITORY hexagons in it.")
          valid = false
        }
        continue
      }

      val capitalCount = connectedHexes.count { this.getData(it).piece is Capital }
      if (capitalCount < 1) {
        Gdx.app.log(
          "Island Validation",
          "There exists a territory with no capital. Hexagon ${hexagon.cubeCoordinate.toAxialKey()} is within it."
        )
        valid = false
      } else if (capitalCount > 1) {
        Gdx.app.log(
          "Island Validation",
          "There exists a territory with more than one capital. Hexagon ${hexagon.cubeCoordinate.toAxialKey()} is within it."
        )
        valid = false
      }

      //TODO check connectedness
    }
    return valid
  }

  fun serialize(): String = Hex.mapper.writeValueAsString(this)

  companion object {
    const val GRID_RADIUS = 20.0

    const val MIN_HEX_IN_TERRITORY = 2

    const val START_CAPITAL = 10

    val PLAYER_TEAM = Team.LEAF

    fun deserialize(json: String): Island {
      return Hex.mapper.readValue(json)
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
        grid.hexagons.mapTo(HashSet()) { it.cubeCoordinate to this.getData(it) }.toMap()
      )

  private data class IslandDTO(
    val width: Int,
    val height: Int,
    val layout: HexagonalGridLayout,
    val hexagonData: Map<CubeCoordinate, HexagonData>
  )
}
