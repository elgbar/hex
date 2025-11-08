package no.elg.hex.util

import com.badlogic.gdx.Gdx
import no.elg.hex.ApplicationArgumentsParser
import no.elg.hex.Hex
import no.elg.hex.hexagon.BARON_STRENGTH
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.HexagonData.Companion.EDGE_DATA
import no.elg.hex.hexagon.HexagonData.Companion.isEdgeHexagon
import no.elg.hex.hexagon.KNIGHT_STRENGTH
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.PEASANT_STRENGTH
import no.elg.hex.hexagon.PalmTree
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.PineTree
import no.elg.hex.hexagon.SPEARMAN_STRENGTH
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.TreePiece
import no.elg.hex.hexagon.mergedType
import no.elg.hex.hexagon.replaceWithTree
import no.elg.hex.island.Island
import no.elg.hex.island.Territory
import no.elg.hex.screens.PlayableIslandScreen.Companion.CASTLE_PRICE
import no.elg.hex.screens.PlayableIslandScreen.Companion.PEASANT_PRICE
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import kotlin.contracts.contract
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.sequences.forEach

/** @return HexagonData of this hexagon */
fun Island.getData(hexagon: Hexagon<HexagonData>): HexagonData =
  hexagon.satelliteData.orElseGet {
    (if (isEdgeHexagon(hexagon) || !Hex.mapEditor) EDGE_DATA else HexagonData(disabled = true, Team.EARTH)).also {
      hexagon.setSatelliteData(it)
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
    if (data.edge || (!Hex.mapEditor && data.invisible)) null else it.get()
  }
}

/**
 * @param hexagon The source hexagon
 * @param team The team to test, if `null` all teams are checked
 *
 * @return All (visible) connected hexagons to the start hexagon of the same team.
 */
fun Island.connectedTerritoryHexagons(hexagon: Hexagon<HexagonData>, team: Team? = this.getData(hexagon).team): Set<Hexagon<HexagonData>> {
  fun connectedTerritoryHexagons(center: Hexagon<HexagonData>, visited: MutableSet<Hexagon<HexagonData>>, island: Island): Set<Hexagon<HexagonData>> {
    val data = island.getData(center)
    // only check a hexagon if they have the same color and haven't been visited
    if (data.invisible || (team != null && data.team != team) || center in visited) {
      return visited
    }

    // add as visited
    visited.add(center)

    // check each neighbor
    for (neighbor in island.grid.getNeighborsOf(center)) {
      connectedTerritoryHexagons(neighbor, visited, island)
    }
    return visited
  }
  return connectedTerritoryHexagons(hexagon, HashSet(), this)
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
  val data = island.getData(this)
  if (data.invisible) {
    return false
  }
  val team = data.team
  for (neighbor in island.getNeighbors(this, onlyVisible = true)) {
    val neighborData = island.getData(neighbor)
    if (neighborData.team == team) {
      return true
    }
  }
  return false
}

/** Get all neighbors of the given [hexagon], not including [hexagon] */
fun Island.getNeighbors(hexagon: Hexagon<HexagonData>, onlyVisible: Boolean = true): Collection<Hexagon<HexagonData>> =
  grid.getNeighborsOf(hexagon).let {
    if (onlyVisible) {
      it.filter { hex -> getData(hex).visible }
    } else {
      it
    }
  }

/**
 * What kind of tree should be on the given hexagon
 */
fun Island.idealTreeType(hexagon: Hexagon<HexagonData>): KClass<out TreePiece> {
  val neighbors: Collection<Hexagon<HexagonData>> = getNeighbors(hexagon, false)
  return if (neighbors.any { getData(it).invisible }) PalmTree::class else PineTree::class
}

/**
 * @param T The tree type to check for
 * @return Whether the given [hexagon] should be of given tree type [T]. NOT if the piece on the hexagon is of type [T]
 */
inline fun <reified T : TreePiece> Island.isIdealTreeType(hexagon: Hexagon<HexagonData>): Boolean = idealTreeType(hexagon) == T::class

/**
 * The strength of a hexagon is how much [Piece.strength] is needed to take a given hexagon. A hexagon defends its surrounding hexes.
 *
 * @param pretendedTeam The team to pretend to be, if null then use the actual team. Useful for calculated potential strength if it was another team
 * @param filter A filter if the given hexagon should be filtered out as a neighbor. The team of the data is guaranteed to be the same as [pretendedTeam] or the actual team
 * @return the strength of the given hexagon
 */
fun Island.calculateStrength(hexagon: Hexagon<HexagonData>, pretendedTeam: Team? = null, filter: ((data: HexagonData) -> Boolean)? = null): Int {
  val data = getData(hexagon)
  val team = pretendedTeam ?: data.team
  val neighborStrength =
    getNeighbors(hexagon)
      .asSequence()
      .map { getData(it) }
      .filter { it.team == team }
      .filter { filter == null || filter(it) }
      .maxOfOrNull { it.piece.strength }
      ?: Empty.strength
  return if (filter?.invoke(data) == false) {
    neighborStrength
  } else {
    max(data.piece.strength, neighborStrength)
  }
}

fun Island.regenerateCapitals() {
  forEachPieceType<Capital> { _, data, _ -> data.setPiece<Empty>() }
  ensureCapitalStartFunds()
  select(null)
}

fun Island.fixWrongTreeTypes() {
  forEachPieceType<TreePiece> { hex, _, _ -> replaceWithTree(this, hex) }
}

fun Island.cleanPiecesOnInvisibleHexagons() {
  invisibleHexagons.withData(this, false) { _, data ->
    data.setPiece<Empty>()
  }
}

fun Island.shuffleAllTeams() {
  val allData = allHexagons
    .asSequence()
    .map(this::getData)

  allData
    .filter { it.piece is Capital }
    .forEach { it.setPiece<Empty>() }

  allData.forEach { it.team = Team.entries.random() }
  regenerateCapitals()
}

fun Island.removeSmallerIslands() {
  val islands = findIslands()
  val maxIsland = islands.maxByOrNull { it.size } ?: return
  for (islandland in islands) {
    if (islandland === maxIsland) continue
    for (hexagon in islandland) {
      getData(hexagon).isDisabled = true
    }
  }

  recalculateVisibleIslands()
}

fun Island.findIslands(): Set<Set<Hexagon<HexagonData>>> {
  val checkedHexagons = HashSet<Hexagon<HexagonData>>()
  val islands = HashSet<Set<Hexagon<HexagonData>>>()

  for (hexagon in visibleHexagons) {
    if (hexagon in checkedHexagons) continue

    val connectedHexes = this.connectedTerritoryHexagons(hexagon, team = null)
    checkedHexagons.addAll(connectedHexes)

    islands.add(connectedHexes)
  }
  return islands
}

fun Island.ensureCapitalStartFunds() {
  reportTiming("ensure capital start funds", 100) {
    for (hexagon in visibleHexagons) {
      val (_, capital, territoryHexagons) = findTerritory(hexagon) ?: continue
      if (capital.balance == 0) {
        capital.balance = capital.calculateStartCapital(territoryHexagons, this)
      }
    }
  }
}

fun Island.canAttack(hexagon: Hexagon<HexagonData>, strength: Int): Boolean = strength > min(calculateStrength(hexagon), KNIGHT_STRENGTH)
fun Island.canAttack(hexagon: Hexagon<HexagonData>, with: Piece): Boolean = canAttack(hexagon, with.strength)

inline fun Island.filterByData(crossinline filter: (data: HexagonData) -> Boolean): Sequence<Hexagon<HexagonData>> =
  visibleHexagons.asSequence().filter { hexagon -> filter(getData(hexagon)) }

inline fun Sequence<Hexagon<HexagonData>>.filterByData(island: Island, crossinline filter: (data: HexagonData) -> Boolean): Sequence<Hexagon<HexagonData>> =
  filter { hexagon -> filter(island.getData(hexagon)) }

/**
 * @param island The island to check on
 * @param team The team to check for, if `null` then all teams are ok
 * @return A function that checks if a hexagon is of a given type and team
 */
inline fun <reified T : Piece> checkIsPieceAndTeam(island: Island, team: Team? = null): ((hexagon: Hexagon<HexagonData>) -> Boolean) =
  { hexagon ->
    val data = island.getData(hexagon)
    data.piece is T && (team == null || data.team == team)
  }

inline fun <reified T : Piece> Hexagon<HexagonData>.isPiece(island: Island, team: Team? = null): Boolean = checkIsPieceAndTeam<T>(island, team).invoke(this)
inline fun <reified T : Piece> Island.filterIsPiece(team: Team? = null): Sequence<Hexagon<HexagonData>> = visibleHexagons.asSequence().filter(checkIsPieceAndTeam<T>(this, team))
inline fun <reified T : Piece> Territory.filterIsPiece(team: Team? = null): Sequence<Hexagon<HexagonData>> = hexagons.asSequence().filter(checkIsPieceAndTeam<T>(island, team))
inline fun <reified T : Piece> Sequence<Hexagon<HexagonData>>.filterIsPiece(island: Island): Sequence<Hexagon<HexagonData>> = filter(checkIsPieceAndTeam<T>(island, null))

inline fun <reified T : Piece> Sequence<Hexagon<HexagonData>>.filterIsPieceAndTeam(island: Island, team: Team): Sequence<Hexagon<HexagonData>> =
  filter(checkIsPieceAndTeam<T>(island, team))

inline fun <reified T : Piece> Collection<Hexagon<HexagonData>>.filterIsPiece(island: Island, team: Team? = null): Collection<Hexagon<HexagonData>> =
  filter(checkIsPieceAndTeam<T>(island, team))

fun Sequence<Hexagon<HexagonData>>.filterIsTeam(island: Island, team: Team): Sequence<Hexagon<HexagonData>> = filter { hexagon -> island.getData(hexagon).team == team }
fun Sequence<Hexagon<HexagonData>>.filterIsNotTeam(island: Island, team: Team): Sequence<Hexagon<HexagonData>> = filter { hexagon -> island.getData(hexagon).team != team }

fun Sequence<Hexagon<HexagonData>>.filterVisible(island: Island): Sequence<Hexagon<HexagonData>> = filter { hexagon -> island.getData(hexagon).visible }
fun Sequence<Hexagon<HexagonData>>.filterInvisible(island: Island): Sequence<Hexagon<HexagonData>> = filter { hexagon -> island.getData(hexagon).invisible }

inline fun Sequence<Hexagon<HexagonData>>.filter(island: Island, crossinline action: (hex: Hexagon<HexagonData>, data: HexagonData) -> Boolean): Sequence<Hexagon<HexagonData>> =
  filter { hexagon -> action(hexagon, island.getData(hexagon)) }

inline fun Sequence<Hexagon<HexagonData>>.none(island: Island, crossinline action: (hex: Hexagon<HexagonData>, data: HexagonData) -> Boolean): Boolean {
  contract { callsInPlace(action) }
  return none { hexagon ->
    action(hexagon, island.getData(hexagon))
  }
}

inline fun Sequence<Hexagon<HexagonData>>.any(island: Island, crossinline action: (hex: Hexagon<HexagonData>, data: HexagonData) -> Boolean): Boolean {
  contract { callsInPlace(action) }
  return any { hexagon ->
    action(hexagon, island.getData(hexagon))
  }
}

inline fun Sequence<Hexagon<HexagonData>>.all(island: Island, crossinline action: (hex: Hexagon<HexagonData>, data: HexagonData) -> Boolean): Boolean {
  contract { callsInPlace(action) }
  return all { hexagon ->
    action(hexagon, island.getData(hexagon))
  }
}

inline fun <reified T : Piece> Island.forEachPieceType(action: (hex: Hexagon<HexagonData>, data: HexagonData, piece: T) -> Unit) {
  for (hexagon in visibleHexagons) {
    val data = getData(hexagon)
    if (data.piece is T) action(hexagon, data, data.piece as T)
  }
}

inline fun Sequence<Hexagon<HexagonData>>.withData(island: Island, excludeInvisible: Boolean = true, crossinline action: (hex: Hexagon<HexagonData>, data: HexagonData) -> Unit) {
  contract { callsInPlace(action) }
  for ((hexagon, data) in this.map { it to island.getData(it) }) {
    if (excludeInvisible && data.invisible) continue
    action(hexagon, data)
  }
}

inline fun Iterable<Hexagon<HexagonData>>.withData(island: Island, excludeInvisible: Boolean = true, crossinline action: (hex: Hexagon<HexagonData>, data: HexagonData) -> Unit) {
  contract { callsInPlace(action) }
  for ((hexagon, data) in this.map { it to island.getData(it) }) {
    if (excludeInvisible && data.invisible) continue
    action(hexagon, data)
  }
}

fun Island.getTerritories(team: Team): Collection<Territory> {
  val territories = HashSet<Territory>()
  val visitedHexagons = HashSet<Hexagon<HexagonData>>()
  reportTiming("get all ${territories.size} territories of team $team", 10) {
    visibleHexagons.withData(this) { hexagon, data ->
      if (data.team == team && hexagon !in visitedHexagons) {
        findTerritory(hexagon)?.also {
          territories.add(it)
          visitedHexagons.addAll(it.hexagons)
        }
      }
    }
  }
  return territories
}

fun Island.getAllTerritories(): HashMap<Team, Collection<Territory>> {
  val visitedHexagons = HashSet<Hexagon<HexagonData>>()
  val territories = HashMap<Team, Collection<Territory>>()

  reportTiming("get all ${territories.map { it.value.size }.sum()} territories of island", 100) {
    visibleHexagons.withData(this) { hexagon, data ->
      if (hexagon in visitedHexagons) return@withData
      val teamTerritories = territories.computeIfAbsent(data.team) { mutableSetOf() } as MutableSet<Territory>
      findTerritory(hexagon)?.also {
        visitedHexagons.addAll(it.hexagons)
        teamTerritories += it
      }
    }
  }
  return territories
}

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

/**
 * How many hexagons normally surrounds a hexagon when all it's neighbors are visible
 */
const val NORMAL_NEIGHBOR_COUNT = 6

fun getNeighborCoordinateByIndex(coordinate: CubeCoordinate, index: Int) =
  CubeCoordinate.fromCoordinates(
    coordinate.gridX + NEIGHBORS[index][NEIGHBOR_X_INDEX],
    coordinate.gridZ + NEIGHBORS[index][NEIGHBOR_Z_INDEX]
  )

fun Island.calculateHexagonsWithinRadius(hexagon: Hexagon<HexagonData>, radius: Int, includeThis: Boolean = true): Set<Hexagon<HexagonData>> {
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

/**
 * Set all hexagons to the given team
 */
fun Island.fill(team: Team) {
  for (hexagon in visibleHexagons) {
    val data = getData(hexagon)
    if (data.team == team) {
      continue
    }
    data.team = team
    if (data.piece !is TreePiece) {
      data.setPiece<Empty>()
    }
  }
  findOrCreateCapital(visibleHexagons)
  select(null)
}

/**
 * List of all hexagons that the current team can do an action on
 */
fun Island.actionableHexagons(): Sequence<Hexagon<HexagonData>> {
  val currentTeam = currentTeam
  return visibleHexagons.asSequence().filter { hexagon ->
    val data = getData(hexagon)
    val piece = data.piece

    if (data.team != currentTeam) {
      // not our team
      return@filter false
    } else if (piece is Capital) {
      val balance = piece.balance
      if (balance < PEASANT_PRICE) {
        // We cannot afford anything
        return@filter false
      }

      val strength = when {
        balance < PEASANT_PRICE * 2 -> PEASANT_STRENGTH
        balance in PEASANT_PRICE * 2 until PEASANT_PRICE * 3 -> SPEARMAN_STRENGTH
        balance in PEASANT_PRICE * 3 until PEASANT_PRICE * 4 -> KNIGHT_STRENGTH
        else -> BARON_STRENGTH
      }

      val territory = findTerritory(hexagon)
      if (territory == null) {
        Gdx.app.error("ISLAND", "Hexagon ${hexagon.coordinates} is not a part of a territory!")
        return@filter false
      }
      val cannotBuyAndAttackAnything = territory.enemyBorderHexes.none { hex -> canAttack(hex, strength) }
      val cannotBuyAndPlaceCastle = balance < CASTLE_PRICE || territory.hexagons.none { hex -> getData(hex).piece is Empty }
      if (cannotBuyAndAttackAnything && cannotBuyAndPlaceCastle) {
        return@filter false
      }
    } else if (piece is LivingPiece) {
      if (piece.moved) {
        // If the piece has moved, it cannot make another move!
        return@filter false
      }

      val territory = findTerritory(hexagon)
      if (territory == null) {
        Gdx.app.error("ISLAND", "Hexagon ${hexagon.coordinates} is not a part of a territory!")
        return@filter false
      }
      val canNotAttackAnything = territory.enemyBorderHexes.none { hex -> canAttack(hex, piece) }
      val canNotMergeWithOtherPieceOrChopTree = territory.hexagons.none {
        val terrPiece = getData(it).piece
        return@none if (terrPiece === piece) {
          false // can never merge with self
        } else if (terrPiece is TreePiece) {
          true // can always cut down trees
        } else {
          // Check if we can and should merge with the pieces
          terrPiece is LivingPiece && piece.canMerge(terrPiece) && mergedType(piece, terrPiece).createHandInstance().price <= territory.capital.income
        }
      }
      if (canNotAttackAnything && canNotMergeWithOtherPieceOrChopTree) {
        // The current piece is able to move, but not attack any territory, nor buy any new pieces to merge with
        return@filter false
      }
    } else {
      // any other piece should be ignored
      return@filter false
    }

    // We can do an action!
    return@filter true
  }
}

val Hexagon<*>.coordinates get() = cubeCoordinate.toAxialKey()