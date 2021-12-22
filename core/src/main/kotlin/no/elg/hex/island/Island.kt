package no.elg.hex.island

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.ObjectIntMap
import com.badlogic.gdx.utils.Queue
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.ai.AI
import no.elg.hex.ai.NotAsRandomAI
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.hexagon.HexagonData.Companion.EDGE_DATA
import no.elg.hex.hexagon.LivingPiece
import no.elg.hex.hexagon.Piece
import no.elg.hex.hexagon.Team
import no.elg.hex.hexagon.Team.EARTH
import no.elg.hex.hexagon.Team.FOREST
import no.elg.hex.hexagon.Team.LEAF
import no.elg.hex.hexagon.Team.STONE
import no.elg.hex.hexagon.Team.SUN
import no.elg.hex.hexagon.TreePiece
import no.elg.hex.hud.MessagesRenderer.publishError
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.island.Island.IslandDto.Companion.createDtoCopy
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.calculateRing
import no.elg.hex.util.connectedHexagons
import no.elg.hex.util.createInstance
import no.elg.hex.util.ensureCapitalStartFunds
import no.elg.hex.util.getAllTerritories
import no.elg.hex.util.getByCubeCoordinate
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.isLazyInitialized
import no.elg.hex.util.next
import no.elg.hex.util.toEnumValue
import no.elg.hex.util.trace
import no.elg.hex.util.treeType
import no.elg.hex.util.withData
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonOrientation.FLAT_TOP
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout
import java.util.EnumMap
import kotlin.math.max

/** @author Elg */
class Island(
  width: Int,
  height: Int,
  layout: HexagonalGridLayout,
  selectedCoordinate: CubeCoordinate? = null,
  @JsonAlias("piece")
  handPiece: Piece? = null,
  hexagonData: Map<CubeCoordinate, HexagonData> = emptyMap(),
  turn: Int = 1,
  initialLoad: Boolean = true
) {
  var turn = turn
    private set
  lateinit var grid: HexagonalGrid<HexagonData>
    private set

  val history = IslandHistory(this)

  /** Prefer this over calling [grid.hexagons] as this has better performance */
  val hexagons: MutableSet<Hexagon<HexagonData>> = HashSet()

  /**
   * Number of visible hexagons on this island. Should not be used while in map editor mode as it is only counted once
   */
  private val visibleHexagons by lazy { hexagons.count { !getData(it).invisible } }

  var hand: Hand? = null
    set(value) {
      field?.apply {
        if (currentHand) {
          dispose()
        }
      }
      if (field != value) {
        field = value
        history.remember("Switch piece")
      }
    }
    get() {
      if (field?.currentHand == false) {
        Gdx.app.log("HAND", "Current hand field has been disposed, but not removed!")
        field = null
      }
      return field
    }

  internal fun restoreState(dto: IslandDto) {
    restoreState(dto.width, dto.height, dto.layout, dto.selectedCoordinate, dto.handPiece, dto.hexagonData, false)
  }

  private fun restoreState(
    width: Int,
    height: Int,
    layout: HexagonalGridLayout,
    selectedCoordinate: CubeCoordinate? = null,
    handPiece: Piece? = null,
    hexagonData: Map<CubeCoordinate, HexagonData> = emptyMap(),
    initialLoad: Boolean,
  ) {
    val builder =
      HexagonalGridBuilder<HexagonData>()
        .setGridWidth(width)
        .setGridHeight(height)
        .setGridLayout(layout)
        .setOrientation(FLAT_TOP)
        .setRadius(GRID_RADIUS)

    grid = builder.build()

    if (hexagonData.isNotEmpty()) {
      for ((coord, data) in hexagonData) {
        grid.getByCubeCoordinate(coord).ifPresent { it.setSatelliteData(data) }
      }
    }
    hexagons.clear()
    hexagons.addAll(grid.hexagons.toSet())

    for (hexagon in hexagons) {
      val data = this.getData(hexagon)
      require(data.piece == Empty || data == data.piece.data) {
        "Found a mismatch between the piece team and the hexagon data team! FML coords ${hexagon.cubeCoordinate.toAxialKey()}"
      }
    }

    if (::hexagonsPerTeam.isLazyInitialized) {
      countHexagons(hexagonsPerTeam)
    }

    recountTotalTreasury()

    if (initialLoad) {
      ensureCapitalStartFunds()
      for (hexagon in hexagons) {
        val data = this.getData(hexagon)
        if (data.invisible) continue
        when (val hexPiece = data.piece) {
          is TreePiece -> hexPiece.hasGrown = false
        }
      }
    }

    val selectedHex = grid.getByCubeCoordinate(selectedCoordinate)
    select(selectedHex)
    val territory = selected
    if (handPiece != null && territory != null) {
      val handData = if (handPiece.data.edge) {
        EDGE_DATA
      } else {
        val newHex = selectedHex ?: error("Failed to find the correct data of hand piece within the territory")
        getData(newHex)
      }
      hand = Hand(
        territory,
        handPiece::class.createInstance(handData).also {
          if (it is LivingPiece) it.moved = false
        }
      )
    }
  }

  val hexagonsPerTeam by lazy { ObjectIntMap<Team>(Team.values().size).apply { countHexagons(this) } }

  val totalBalancePerTeam: ObjectIntMap<Team> = ObjectIntMap<Team>(Team.values().size)
    get() {
      synchronized(SYNC) {
        return field
      }
    }
  val totalIncomePerTeam: ObjectIntMap<Team> = ObjectIntMap<Team>(Team.values().size)
    get() {
      synchronized(SYNC) {
        return field
      }
    }

  private val initialState: IslandDto

  var selected: Territory? = null
    private set

  /**
   * How many of the current players are real (i.e. no an AI)
   */
  val realPlayers: Int get() = teamToPlayer.count { (_, ai) -> ai == null }

  /**
   * How many of the current players are not real (i.e. an AI)
   */
  val aiPlayers: Int get() = Team.values().size - realPlayers

  val currentAI: AI? get() = teamToPlayer[currentTeam]

  var currentTeam: Team = Settings.startTeam
    private set

  private val teamToPlayer =
    EnumMap<Team, AI?>(Team::class.java).apply {
      // if we create more teams this makes sure they are playing along
      this.putAll(Team.values().map { it to NotAsRandomAI(it) })

      put(SUN, Settings.teamSunAI.aiConstructor(SUN))
      put(LEAF, Settings.teamLeafAI.aiConstructor(LEAF))
      put(FOREST, Settings.teamForestAI.aiConstructor(FOREST))
      put(EARTH, Settings.teamEarthAI.aiConstructor(EARTH))
      put(STONE, Settings.teamStoneAI.aiConstructor(STONE))
    }

  init {
    restoreState(width, height, layout, selectedCoordinate, handPiece, hexagonData, initialLoad)
    history.clear()
    initialState = createDto().copy()
  }

  inline fun isCurrentTeamAI() = currentAI != null
  inline fun isCurrentTeamHuman() = currentAI == null

  // ////////// //
  //  Gameplay  //
  // ////////// //

  /**
   * SLOW!
   */
  internal fun recountTotalTreasury() {
    synchronized(SYNC) {
      totalIncomePerTeam.clear(Team.values().size)
      totalBalancePerTeam.clear(Team.values().size)
      for ((team, territory) in getAllTerritories()) {
        totalIncomePerTeam.getAndIncrement(team, 0, territory.sumBy { it.income })
        totalBalancePerTeam.getAndIncrement(team, 0, territory.sumBy { it.capital.balance })
      }
    }
  }

  private fun countHexagons(counter: ObjectIntMap<Team>) {
    counter.clear(Team.values().size)
    hexagons.withData(this) { _, data ->
      counter.getAndIncrement(data.team, 0, 1)
    }
  }

  fun percentagesHexagons(): EnumMap<Team, Float> {
    val hexes = hexagonsPerTeam
    val totalHexagons = visibleHexagons.toFloat()
    return Team.values().associateWithTo(EnumMap<Team, Float>(Team::class.java)) {
      hexes.get(it, 0) / totalHexagons
    }
  }

  fun endTurn(gameInputProcessor: GameInputProcessor) {

    history.disable()
    history.clear()

    KtxAsync.launch(Hex.asyncThread) {

      currentTeam = Team.values().next(currentTeam)
      Gdx.app.debug("TURN", "Starting turn of $currentTeam")

      for (hexagon in hexagons) {
        if (gameInputProcessor.screen.checkEndedGame()) {
          return@launch
        }
        val data = this@Island.getData(hexagon)
        if (data.team != currentTeam || data.invisible) continue
        data.piece.beginTurn(this@Island, hexagon, data, currentTeam)
      }

      if (currentTeam == Settings.startTeam) {
        Gdx.app.debug("TURN", "New round!")
        turn++
        for (hexagon in hexagons) {
          val data = this@Island.getData(hexagon)
          if (data.invisible) continue
          data.piece.newRound(this@Island, hexagon)
        }
      }
      select(null)
      beginTurn(gameInputProcessor)
    }
  }

  fun beginTurn(gameInputProcessor: GameInputProcessor) {
    KtxAsync.launch(Hex.asyncThread) {
      val cai = currentAI
      if (cai != null) {

        try {
          cai.action(this@Island, gameInputProcessor)
        } catch (e: RuntimeException) {
          e.printStackTrace()
          publishError("Exception thrown during AI turn: ${e::class.simpleName} ${e.message}", Float.MAX_VALUE, e)
        }
        delay(50)
        if (Hex.screen is PreviewIslandScreen) {
          endTurn(gameInputProcessor)
        }
      } else {
        // enable history only when it's a humans turn
        history.enable()
        history.clear()

        // Loose when no player have any capitals left
        if (hexagons.none {
          val data = getData(it)
          teamToPlayer[data.team] == null && data.piece is Capital
        }
        ) {
          gameInputProcessor.screen.gameEnded(false)
        }
      }
    }
  }

  fun restoreInitialState() {
    history.clear()
    Hex.screen.also {
      if (it is PreviewIslandScreen) {
        it.clearProgress()
      }
    }
    Hex.screen = LevelSelectScreen
    // only restore state after surrender to make sure the preview is last known state
    restoreState(initialState.copy())
  }

  fun surrender() {
    restoreInitialState()
    Gdx.app.log("ISLAND", "Player surrendered on turn $turn")
  }

  /** Select the hex under the cursor */
  fun select(hexagon: Hexagon<HexagonData>?): Boolean {
    val oldSelected = selected
    hand = null
    selected = null

    Gdx.app.trace("SELECT", "Selecting hexagon ${hexagon?.cubeCoordinate}")

    if (hexagon == null) {
      if (oldSelected != null) {
        history.remember("Unselected territory")
      }
      return true
    }

    val territory = findTerritory(hexagon)
    return if (territory == null) {
      selected = oldSelected
      false
    } else {
      selected = territory
      history.remember("Select territory")
      true
    }
  }

  /**
   * Find the terrorist the given hexagon is connected to. This method also validates that the territory of the hexagon
   *
   * @return The territory the hexagon is part of or `null` if no capital can be found
   */
  fun findTerritory(hexagon: Hexagon<HexagonData>): Territory? {
    val territoryHexes = getTerritoryHexagons(hexagon) ?: return null

    val capital = findCapital(territoryHexes)
    return if (capital != null) Territory(this, capital, territoryHexes) else null
  }

  /**
   * Finds the best capital of the collection of hexagons.
   * This method assumes all hexagons are within the same team, no checks will be done in this regard.
   *
   * * If there are multiple capitals, this method will delete all but the best and transfer its resources.
   * * If there are no capitals a capital will be created at the best location possible
   *
   * @see calculateBestCapitalPlacement
   *
   * @return The best capital within the collection of hexagons.
   */
  fun findCapital(territoryHexes: Collection<Hexagon<HexagonData>>): Capital? {
    val capitals = territoryHexes.filter { this.getData(it).piece is Capital }
    if (capitals.isEmpty()) {
      // No capital found, generate a new capital
      val capHexData = this.getData(calculateBestCapitalPlacement(territoryHexes))
      return if (capHexData.setPiece(Capital::class)) capHexData.piece as Capital else null
    } else if (capitals.size == 1) {
      return this.getData(capitals.first()).piece as Capital
    }
    // No capital found, generate a new capital

    // there might be multiple capitals in the set of hexagons. Find the best one, transfer all
    // assets and delete the others

    val bestCapitalHex = calculateBestCapitalPlacement(capitals)
    val bestData = this.getData(bestCapitalHex).piece as Capital
    for (capital in capitals) {
      if (capital === bestCapitalHex) continue
      val data = this.getData(capital)
      val otherCapital = data.piece
      if (otherCapital !is Capital) {
        Gdx.app.log("FIND CAPITAL", "A piece which was a capital is no longer a capital, but a ${otherCapital::class.simpleName} piece. Data: $data")
        continue
      }
      otherCapital.transfer(bestData)
      data.setPiece(Empty::class)
    }
    return bestData
  }

  /**
   * Get all hexagons that is in the same territory as the given [hexagon] or
   * `null` if hexagon is not a part of a territory
   */
  fun getTerritoryHexagons(hexagon: Hexagon<HexagonData>): Set<Hexagon<HexagonData>>? {
    val territoryHexes = connectedHexagons(hexagon)
    if (territoryHexes.size < MIN_HEX_IN_TERRITORY) {
      // If given hexagon is a capital, but it is no longer a part of a territory (ie it's on its own)
      // then replace the capital with a tree
      val data = getData(hexagon)
      if (data.piece is Capital) {
        data.setPiece(treeType(hexagon))
      }
      return null
    }
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
   * @param hexagons All hexagons in a territory, must have a size equal to or greater than
   * [MIN_HEX_IN_TERRITORY]
   */
  fun calculateBestCapitalPlacement(
    hexagons: Collection<Hexagon<HexagonData>>
  ): Hexagon<HexagonData> {
    require(hexagons.size >= MIN_HEX_IN_TERRITORY) {
      "There must be at least $MIN_HEX_IN_TERRITORY hexagons in the given set!"
    }
    val hexTeam = this.getData(hexagons.first()).team
    require(hexagons.all { this.getData(it).team == hexTeam }) {
      "All hexagons given must be on the same team"
    }

    // Capitals should be prefer a worse location in favor of overwriting another piece
    val maxPlacementPreference = hexagons.minOf { getData(it).piece.capitalPlacement }
    val feasibleHexagons =
      hexagons.filter { getData(it).piece.capitalPlacement <= maxPlacementPreference }

    val contenders = HashSet<Hexagon<HexagonData>>(feasibleHexagons.size)

    // The maximum distance between two hexagons for this grid
    val maxRadius = 3 * max(grid.gridData.gridWidth, grid.gridData.gridHeight) + 1

    fun findDistanceToClosestEnemyHex(hex: Hexagon<HexagonData>, discardIfLessThan: Int): Int {

      for (r in discardIfLessThan..maxRadius) {
        if (this.calculateRing(hex, r).any {
          val data = this.getData(it)
          data.team != hexTeam && !data.invisible
        }
        ) {
          return r
        }
      }
      return NO_ENEMY_HEXAGONS // no hexes found we've won!
    }

    var greatestDistance = 1
    for (hex in feasibleHexagons) {
      val dist = findDistanceToClosestEnemyHex(hex, 1)
      if (dist == NO_ENEMY_HEXAGONS) {
        contenders.addAll(feasibleHexagons)
        break
      }
      if (dist > greatestDistance) {
        // we have a new greatest distance
        greatestDistance = dist
        contenders.clear()
      }
      if (dist == greatestDistance) {
        contenders += hex
      }
    }

    require(contenders.isNotEmpty()) { "No capital contenders found in ${hexagons.map { "@${it.cubeCoordinate.toAxialKey()} | data ${getData(it)}" }}" }

    Gdx.app.trace(
      "ISLAND",
      "There are ${contenders.size} hexes to become capital. Each of them have a minimum radius to other hexagons of $greatestDistance, maxRadius is $maxRadius"
    )
    Gdx.app.trace("ISLAND") {
      "Contenders are ${
      contenders.map {
        "${getData(it)}@${it.cubeCoordinate.let { coord -> coord.gridX to coord.gridZ }} dist of ${
        findDistanceToClosestEnemyHex(
          it,
          1
        )
        }"
      }
      }"
    }

    if (contenders.size == 1) return contenders.first()

    // if we have multiple contenders to become the capital, select the one with fewest enemy
    // hexagons near it
    // invisible hexagons count to ours hexagons

    var currBest: Hexagon<HexagonData>? = null
    var currBestScore = -1.0

    for (origin in contenders) {
      val ring = this.getNeighbors(origin, onlyVisible = false)
      val calcScore = ring.sumByDouble {
        val data = this.getData(it)
        when {
          data.team == hexTeam -> 1.0
          data.invisible -> 0.5
          else -> 0.0
        }
      }
      if (calcScore > currBestScore) {
        currBestScore = calcScore
        currBest = origin
      }
    }

    Gdx.app.trace("ISLAND") { "Best capital found was ${getData(currBest!!)}@${currBest.cubeCoordinate} with a score of $currBestScore" }

    return currBest ?: error("No best!?")
  }

  // /////////////////
  // Serialization //
  // /////////////////

  /**
   *
   * Validation rules:
   *
   * * All visible hexagons must be reachable from all other visible hexagons (ie there can only be
   * one island)
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
          publishError("Hexagon ${hexagon.cubeCoordinate.toAxialKey()} is a capital, even though it has fewer than $MIN_HEX_IN_TERRITORY hexagons in it.")
          valid = false
        }
        continue
      }

      val capitalCount = connectedHexes.count { this.getData(it).piece is Capital }
      if (capitalCount < 1) {
        publishError("There exists a territory with no capital. Hexagon ${hexagon.cubeCoordinate.toAxialKey()} is within it.")
        valid = false
      } else if (capitalCount > 1) {
        publishError("There exists a territory with more than one capital. Hexagon ${hexagon.cubeCoordinate.toAxialKey()} is within it.")
        valid = false
      }
    }

    // check that every hexagon is connected

    val visibleNeighbors = HashSet<Hexagon<HexagonData>>(hexagons.size)
    val toCheck = Queue<Hexagon<HexagonData>>(hexagons.size * 2)
    toCheck.addFirst(hexagons.first { !getData(it).invisible })

    do {
      val curr: Hexagon<HexagonData> = toCheck.removeFirst()
      if (visibleNeighbors.contains(curr)) continue
      val neighbors = getNeighbors(curr)

      for (neighbor in neighbors) {
        if (visibleNeighbors.contains(neighbor)) continue
        toCheck.addLast(neighbor)
      }
      visibleNeighbors += curr
    } while (!toCheck.isEmpty)

    val allVisibleHexagons = hexagons.filterNot { getData(it).invisible }

    if (!allVisibleHexagons.containsAll(visibleNeighbors) || !visibleNeighbors.containsAll(allVisibleHexagons)) {
      publishError("The visible hexagon grid is not connected.")
      valid = false
    }
    return valid
  }

  companion object {
    const val GRID_RADIUS = 20.0

    const val MIN_HEX_IN_TERRITORY = 2

    const val START_CAPITAL_PER_HEX = 5

    const val NO_ENEMY_HEXAGONS = -1

    private val SYNC = Any()

    fun deserialize(json: String): Island {
      return Hex.mapper.readValue(json)
    }

    fun deserialize(file: FileHandle): Island {
      return deserialize(file.readString())
    }
  }

  // ////////////////////////
  // Data Transfer Object //
  // ////////////////////////

  @JsonValue
  internal fun createDto(): IslandDto {
    // prefer coordinates of held piece if nothing is held select any hexagon within the selected territory
    val coord: CubeCoordinate? =
      (
        hand?.piece?.data?.let { data ->
          // slight edge case (pun intended) if we hold a piece that is a hand instance, do not record its coordinates
          if (data.edge) null else hexagons.find { hex -> getData(hex) === data }
        } ?: selected?.hexagons?.first()
        )?.cubeCoordinate

    return IslandDto(
      grid.gridData.gridWidth,
      grid.gridData.gridHeight,
      grid.gridData.gridLayout.toEnumValue(),
      coord,
      hand?.piece?.createDtoCopy(),
      hexagons.mapTo(HashSet()) { it.cubeCoordinate to getData(it).copy() }.toMap(),
      turn,
      false
    )
  }

  data class IslandDto(
    val width: Int,
    val height: Int,
    val layout: HexagonalGridLayout,
    val selectedCoordinate: CubeCoordinate? = null,
    val handPiece: Piece? = null,
    val hexagonData: Map<CubeCoordinate, HexagonData>,
    val turn: Int,
    val initialLoad: Boolean
  ) {
    fun copy(): IslandDto {
      return IslandDto(
        width,
        height,
        layout,
        selectedCoordinate,
        handPiece?.createDtoCopy(),
        hexagonData.mapValues { (_, data) -> data.copy() },
        turn,
        false
      )
    }

    companion object {
      internal fun Piece?.createDtoCopy(): Piece? {
        return this?.let { it.copyTo(it.data.copy()) }
      }
    }
  }
}
