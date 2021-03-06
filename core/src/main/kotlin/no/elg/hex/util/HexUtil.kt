package no.elg.hex.util

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
import java.util.HashSet
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

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

/** @return All visible hexagons connected to the start hexagon of the same team */
fun Island.connectedHexagons(hexagon: Hexagon<HexagonData>, team: Team? = this.getData(hexagon).team): Set<Hexagon<HexagonData>> {
  return connectedHexagons(hexagon, team, HashSet(), this)
}

/**
 * @param team The team to test, if null all teams are cheked
 */
private fun connectedHexagons(
  center: Hexagon<HexagonData>,
  team: Team?,
  visited: MutableSet<Hexagon<HexagonData>>,
  island: Island
): Set<Hexagon<HexagonData>> {
  val data = island.getData(center)
  // only check a hexagon if they have the same color and haven't been visited
  if (visited.contains(center) || (team != null && data.team != team) || data.invisible) {
    return visited
  }

  // add as visited
  visited.add(center)

  // check each neighbor
  for (neighbor in island.grid.getNeighborsOf(center)) {
    connectedHexagons(neighbor, team, visited, island)
  }
  return visited
}

/** Get all neighbors of the given [hexagon], not including [hexagon] */
fun Island.getNeighbors(hexagon: Hexagon<HexagonData>, onlyVisible: Boolean = true) =
  grid.getNeighborsOf(hexagon).let {
    if (onlyVisible) {
      it.filter { hex -> !getData(hex).invisible }
    } else {
      it
    }
  }

fun Island.treeType(hexagon: Hexagon<HexagonData>): KClass<out TreePiece> {
  val neighbors: Collection<Hexagon<HexagonData>> = getNeighbors(hexagon, false)
  return if (neighbors.any { getData(it).invisible }) PalmTree::class else PineTree::class
}

fun Island.calculateStrength(hexagon: Hexagon<HexagonData>): Int {
  val data = getData(hexagon)
  val team = data.team
  val neighborStrength =
    getNeighbors(hexagon)
      .map { getData(it) }
      .filter { it.team == team }
      .map { it.piece.strength }
      .maxOrNull()
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

    val connectedHexes = this.connectedHexagons(hexagon, team = null)
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

fun Island.canAttack(hexagon: Hexagon<HexagonData>, with: Piece): Boolean {
  return with.strength > min(calculateStrength(hexagon), KNIGHT_STRENGTH)
}

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
  action: (hex: Hexagon<HexagonData>, data: HexagonData) -> Unit
) {
  for (hexagon in this) {
    action(hexagon, island.getData(hexagon))
  }
}

fun Island.getTerritories(team: Team): Collection<Territory> {
  val territories = HashSet<Territory>()
  hexagons.withData(this) { hexagon, data ->
    if (data.team == team) {
      select(hexagon)
      selected?.also { territories.add(it) }
    }
    select(null)
  }
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
