package no.elg.hex.util

import com.badlogic.gdx.Gdx
import no.elg.hex.ApplicationArgumentsParser
import no.elg.hex.Hex
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.HexagonData.Companion.EDGE_DATA
import no.elg.hex.hexagon.HexagonData.Companion.isEdgeHexagon
import no.elg.hex.hexagon.KNIGHT_STRENGTH
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.TreePiece
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

/** @return HexagonData of this hexagon */
fun Island.getData(hexagon: Hexagon<HexagonData>): HexagonData {
  return hexagon.satelliteData.orElseGet {
    (if (isEdgeHexagon(hexagon)) EDGE_DATA else HexagonData()).also {
      hexagon.setSatelliteData(it)
    }
  }
}

/**
 * @param x screen x
 * @param y screen y
 *
 * Note that if the application is in map editor mode (via [ApplicationArgumentsParser.mapEditor])
 * this will return opaque hexagons. If not however these will be invisible to this method.
 *
 * @return Get the hexagon at a given screen location or `null` if nothing is found.
 */
fun Island.getHexagon(x: Double, y: Double): Hexagon<HexagonData>? {
  return this.grid.getByPixelCoordinate(x, y).let {
    if (it.isEmpty()) return@let null
    val data = this.getData(it.get())
    if (data.edge || (!Hex.args.mapEditor && data.invisible)) null else it.get()
  }
}

/**
 * @param hexagon The source hexagon
 * @param team The team to test, if `null` all teams are checked
 *
 * @return All (visible) connected hexagons to the start hexagon of the same team.
 */
fun Island.connectedTerritoryHexagons(
  hexagon: Hexagon<HexagonData>,
  team: Team? = this.getData(hexagon).team
): Set<Hexagon<HexagonData>> {
  return connectedTerritoryHexagons(hexagon, team, HashSet(), this)
}

/**
 * @param team The team to test, if null all teams are checked
 */
private fun connectedTerritoryHexagons(
  center: Hexagon<HexagonData>,
  team: Team?,
  visited: MutableSet<Hexagon<HexagonData>>,
  island: Island
): Set<Hexagon<HexagonData>> {
  val data = island.getData(center)
  // only check a hexagon if they have the same color and haven't been visited
  if (center in visited || (team != null && data.team != team) || data.invisible) {
    return visited
  }

  // add as visited
  visited.add(center)

  // check each neighbor
  for (neighbor in island.grid.getNeighborsOf(center)) {
    connectedTerritoryHexagons(neighbor, team, visited, island)
  }
  return visited
}

/**
 * If the given hexagon is NOT a part of a territory.
 * That is, the given hexagon does not have a neighbor hexagon which is in on the same team as the given hexagon
 */
fun Hexagon<HexagonData>.isNotPartOfATerritory(island: Island): Boolean = !isPartOfATerritory(island)

/**
 * If the given hexagon is a part of a territory.
 * That is, the given hexagon have a neighbor hexagon which is in on the same team as the given hexagon
 */
fun Hexagon<HexagonData>.isPartOfATerritory(island: Island): Boolean {
  val team = island.getData(this).team
  for (neighbor in island.getNeighbors(this)) {
    val data = island.getData(neighbor)
    if (data.team == team) {
      return true
    }
  }
  return false
}

/** Get all neighbors of the given [hexagon], not including [hexagon] */
fun Island.getNeighbors(hexagon: Hexagon<HexagonData>, onlyVisible: Boolean = true) =
  grid.getNeighborsOf(hexagon).let {
    if (onlyVisible) {
      it.filterNot { hex -> getData(hex).invisible }
    } else {
      it
    }
  }

fun Island.treeType(hexagon: Hexagon<HexagonData>): KClass<out TreePiece> {
  val neighbors: Collection<Hexagon<HexagonData>> = getNeighbors(hexagon, false)
  return if (neighbors.any { getData(it).invisible }) PalmTree::class else PineTree::class
}

/**
 * The strength of a hexagon is how much [Piece.strength] is needed to take a given hexagon. A hexagon defends its surrounding hexes.
 *
 * @param pretendedTeam The team to pretend to be, if null then use the actual team. Useful for calculated potential strength if it was another team
 * @return the strength of the given hexagon
 */
fun Island.calculateStrength(hexagon: Hexagon<HexagonData>, pretendedTeam: Team? = null): Int {
  val data = getData(hexagon)
  val team = pretendedTeam ?: data.team
  val neighborStrength =
    getNeighbors(hexagon)
      .map { getData(it) }
      .filter { it.team == team }
      .maxOfOrNull { it.piece.strength }
      ?: 0
  return max(data.piece.strength, neighborStrength)
}

fun Island.regenerateCapitals() {
  forEachPieceType<Capital> { _, data, _ -> data.setPiece(Empty::class) }
  ensureCapitalStartFunds()
  select(null)
}

fun Island.findIslands(): Set<Set<Hexagon<HexagonData>>> {
  val checkedHexagons = HashSet<Hexagon<HexagonData>>()
  val islands = HashSet<Set<Hexagon<HexagonData>>>()

  for (hexagon in hexagons) {
    if (hexagon in checkedHexagons || this.getData(hexagon).invisible) continue

    val connectedHexes = this.connectedTerritoryHexagons(hexagon, team = null)
    checkedHexagons.addAll(connectedHexes)

    islands.add(connectedHexes)
  }
  return islands
}

fun Island.ensureCapitalStartFunds() {
  for (hexagon in hexagons) {
    val (_, capital, territoryHexagons) = findTerritory(hexagon) ?: continue
    if (capital.balance == 0) {
      capital.balance = capital.calculateStartCapital(territoryHexagons, this)
    }
  }
}

fun Island.canAttack(hexagon: Hexagon<HexagonData>, strength: Int): Boolean = strength > min(calculateStrength(hexagon), KNIGHT_STRENGTH)
fun Island.canAttack(hexagon: Hexagon<HexagonData>, with: Piece): Boolean = canAttack(hexagon, with.strength)

inline fun <reified T : Piece> Island.forEachPieceType(
  action: (hex: Hexagon<HexagonData>, data: HexagonData, piece: T) -> Unit
) {
  for (hexagon in hexagons) {
    val data = getData(hexagon)
    if (data.piece is T) action(hexagon, data, data.piece as T)
  }
}

fun Iterable<Hexagon<HexagonData>>.withData(
  island: Island,
  ignoreInvisible: Boolean = true,
  action: (hex: Hexagon<HexagonData>, data: HexagonData) -> Unit
) {
  for (hexagon in this) {
    val data = island.getData(hexagon)
    if (ignoreInvisible && data.invisible) continue
    action(hexagon, data)
  }
}

fun Island.getTerritories(team: Team): Collection<Territory> {
  val territories = HashSet<Territory>()
  hexagons.withData(this) { hexagon, data ->
    if (data.team == team) {
      findTerritory(hexagon)?.also { territories.add(it) }
    }
  }
  return territories
}

fun Island.getAllTerritories(): HashMap<Team, Collection<Territory>> {
  val visitedHexagons = HashSet<Hexagon<HexagonData>>()
  val territories = HashMap<Team, Collection<Territory>>()

  val ms = measureTimeMillis {
    visibleHexagons.withData(this) { hexagon, data ->
      if (hexagon in visitedHexagons) return@withData
      val teamTerritories = territories.computeIfAbsent(data.team) { mutableSetOf() } as MutableSet<Territory>
      findTerritory(hexagon)?.also {
        visitedHexagons.addAll(it.hexagons)
        teamTerritories += it
      }
    }
  }
  Gdx.app.debug("TIME") { "Took ${ms / 1000f} to get all ${territories.map { it.value.size }.sum()} territories of island" }

  return territories
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Taken from https://github.com/Hexworks/mixite/pull/56 TODO remove when this is in the library
//                 //
// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

const val NEIGHBOR_X_INDEX = 0

const val NEIGHBOR_Z_INDEX = 1

val NEIGHBORS =
  arrayOf(
    intArrayOf(+1, 0),
    intArrayOf(+1, -1),
    intArrayOf(0, -1),
    intArrayOf(-1, 0),
    intArrayOf(-1, +1),
    intArrayOf(0, +1)
  )

fun getNeighborCoordinateByIndex(coordinate: CubeCoordinate, index: Int) =
  CubeCoordinate.fromCoordinates(
    coordinate.gridX + NEIGHBORS[index][NEIGHBOR_X_INDEX],
    coordinate.gridZ + NEIGHBORS[index][NEIGHBOR_Z_INDEX]
  )

fun Island.calculateHexagonsWithinRadius(
  hexagon: Hexagon<HexagonData>,
  radius: Int,
  includeThis: Boolean = true
): Set<Hexagon<HexagonData>> {
  val result = HashSet<Hexagon<HexagonData>>()
  if (includeThis) {
    result += hexagon
  }
  for (subRadius in 1..radius) {
    result += this.calculateRing(hexagon, subRadius)
  }
  return result
}

fun Island.calculateRing(hexagon: Hexagon<HexagonData>, radius: Int): Set<Hexagon<HexagonData>> {
  val result = HashSet<Hexagon<HexagonData>>()

  var currentCoordinate =
    CubeCoordinate.fromCoordinates(hexagon.gridX - radius, hexagon.gridZ + radius)

  for (i in 0 until 6) {
    for (j in 0 until radius) {
      currentCoordinate = getNeighborCoordinateByIndex(currentCoordinate, i)
      val hex = grid.getByCubeCoordinate(currentCoordinate)
      if (hex.isPresent && !getData(hex.get()).edge) {
        result.add(hex.get())
      }
    }
  }

  return result
}