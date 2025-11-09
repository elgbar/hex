package no.elg.hex.island

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Queue
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIncludeProperties
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.ai.AI
import no.elg.hex.ai.Difficulty
import no.elg.hex.event.Events
import no.elg.hex.event.events.HandChangedEvent
import no.elg.hex.event.events.HexagonVisibilityChanged
import no.elg.hex.event.events.TeamEndTurnEvent
import no.elg.hex.hexagon.Capital
import no.elg.hex.hexagon.Castle
import no.elg.hex.hexagon.Empty
import no.elg.hex.hexagon.Grave
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
import no.elg.hex.hexagon.replaceWithTree
import no.elg.hex.hud.MessagesRenderer
import no.elg.hex.input.GameInteraction
import no.elg.hex.island.Hand.Companion.NoRestore
import no.elg.hex.island.Island.Companion.MIN_HEX_IN_TERRITORY
import no.elg.hex.model.IslandDto
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.calculateRing
import no.elg.hex.util.connectedTerritoryHexagons
import no.elg.hex.util.coordinates
import no.elg.hex.util.createInstance
import no.elg.hex.util.debug
import no.elg.hex.util.forEachPieceType
import no.elg.hex.util.getByCubeCoordinate
import no.elg.hex.util.getData
import no.elg.hex.util.getNeighbors
import no.elg.hex.util.idealTreeType
import no.elg.hex.util.isLazyInitialized
import no.elg.hex.util.isPartOfATerritory
import no.elg.hex.util.next
import no.elg.hex.util.toEnumValue
import no.elg.hex.util.trace
import no.elg.hex.util.withData
import org.hexworks.mixite.core.api.CubeCoordinate
import org.hexworks.mixite.core.api.Hexagon
import org.hexworks.mixite.core.api.HexagonOrientation.FLAT_TOP
import org.hexworks.mixite.core.api.HexagonalGrid
import org.hexworks.mixite.core.api.HexagonalGridBuilder
import org.hexworks.mixite.core.api.HexagonalGridLayout
import java.util.EnumMap
import kotlin.math.max
import kotlin.system.measureTimeMillis

/** @author Elg */
@JsonIncludeProperties("width", "height", "layout", "territoryCoordinate", "handCoordinate", "handPiece", "handRestoreAction", "hexagonData", "round", "startTeam")
class Island(
  width: Int,
  height: Int,
  layout: HexagonalGridLayout,
  territoryCoordinate: CubeCoordinate? = null,
  @JsonAlias("selectedCoordinate")
  handCoordinate: CubeCoordinate? = null,
  @JsonAlias("piece")
  handPiece: Piece? = null,
  handRestoreAction: String? = null,
  hexagonData: Map<CubeCoordinate, HexagonData> = emptyMap(),
  @JsonAlias("turn")
  var round: Int = 1,
  @JsonAlias("team")
  private val startTeam: Team = Settings.startTeam
) {

  /**
   * Each time a player have had a turn, the turn increases by one. In one round there are `Team.entries.size` turns.
   * @return The current turn
   */
  val turn get() = (round - 1) * Team.entries.size + (currentTeam.ordinal - startTeam.ordinal)

  lateinit var grid: HexagonalGrid<HexagonData>
    private set

  val history = IslandHistory(this)
  lateinit var gameInteraction: GameInteraction

  /**
   * Prefer this over calling [HexagonalGrid.hexagons] as this has better performance.
   *
   * During normal play it will only contain the visible hexagons but when [no.elg.hex.ApplicationArgumentsParser.mapEditor] is `true` this will contain all hexagons
   */
  private val internalAllHexagons: MutableSet<Hexagon<HexagonData>> = HashSet()
  private val internalVisibleHexagons: MutableSet<Hexagon<HexagonData>> = HashSet()

  val allHexagons: Set<Hexagon<HexagonData>> get() = internalAllHexagons
  val visibleHexagons: Set<Hexagon<HexagonData>> get() = internalVisibleHexagons
  val invisibleHexagons: Set<Hexagon<HexagonData>> get() = allHexagons - visibleHexagons

  val hexagonCoordinateToData: Map<CubeCoordinate, HexagonData> get() = visibleHexagons.associate { it.cubeCoordinate to getData(it).copy() }

  var hand: Hand? = null
    set(value) {
      field?.also {
        if (it.currentHand) {
          it.dispose()
        }
      }
      if (field != value) {
        val old = field
        field = value
        Events.fireEvent(HandChangedEvent(old?.piece, value?.piece))
        history.remember("Switch piece")
        Gdx.graphics.requestRendering()
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
    // Use copy to avoid modifying the dto passed in
    with(dto.copy()) {
      restoreState(
        width = width,
        height = height,
        layout = layout,
        territoryCoordinate = territoryCoordinate,
        handCoordinate = handCoordinate,
        handPiece = handPiece,
        handRestoreActionName = handRestoreAction,
        hexagonData = hexagonData,
        team = team
      )
    }
  }

  fun recalculateVisibleIslands() {
    internalAllHexagons.clear()
    internalVisibleHexagons.clear()
    internalAllHexagons.addAll(grid.hexagons)

    for (hexagon in allHexagons) {
      val data = getData(hexagon)
      require(data.piece == Empty || data == data.piece.data) {
        "Found a mismatch between the piece team and the hexagon data team! FML coords ${hexagon.coordinates}"
      }
      if (data.visible) {
        internalVisibleHexagons += hexagon
      }
    }

    if (::hexagonsPerTeam.isLazyInitialized) {
      recountHexagons()
    }
  }

  private fun restoreState(
    width: Int,
    height: Int,
    layout: HexagonalGridLayout,
    territoryCoordinate: CubeCoordinate? = null,
    handCoordinate: CubeCoordinate? = null,
    handPiece: Piece? = null,
    handRestoreActionName: String? = null,
    hexagonData: Map<CubeCoordinate, HexagonData> = emptyMap(),
    team: Team
  ) {
    grid =
      HexagonalGridBuilder<HexagonData>()
        .setGridWidth(width)
        .setGridHeight(height)
        .setGridLayout(layout)
        .setOrientation(FLAT_TOP)
        .setRadius(GRID_RADIUS)
        .build()

    for ((coord, data) in hexagonData) {
      grid.getByCubeCoordinate(coord).ifPresent { it.setSatelliteData(data) }
    }
    // This must be after
    recalculateVisibleIslands()

    currentTeam = team

    val territoryHex = grid.getByCubeCoordinate(territoryCoordinate)
    val successfulSelect = select(territoryHex)
    val territory = selected
    if (successfulSelect && handPiece != null && territory != null) {
      val handHex = territory.hexagons.find { it.cubeCoordinate == handCoordinate }
      val handData = handHex?.let(::getData) ?: EDGE_DATA
      val handRestoreAction = Hand.Companion.RestoreAction.fromString(handRestoreActionName)
      hand = Hand(
        territory,
        handPiece::class.createInstance(handData).also {
          if (it is LivingPiece) it.moved = false
        },
        handRestoreAction
      )
    }
  }

  val hexagonsPerTeam by lazy { EnumMap<Team, Int>(Team::class.java).also { recountHexagons(it) } }
  val winningTeam: Team get() = hexagonsPerTeam.maxByOrNull(Map.Entry<Team, Int>::value)?.key ?: LEAF

  var selected: Territory? = null
    private set

  /**
   * How many of the current players are real (i.e. no an AI)
   */
  val realPlayers: Int get() = teamToPlayer.count { (_, ai) -> ai == null }

  /**
   * @return If there is only one real player left
   */
  val singleAliveRealPlayer: Boolean get() = hexagonsPerTeam.filterValues { it > 0 }.keys.filter { isTeamHuman(it) }.size == 1
  val onlyAIPlayers: Boolean get() = realPlayers == 0

  /**
   * How many of the current players are not real (i.e. an AI)
   */
  val aiPlayers: Int get() = Team.entries.size - realPlayers

  val currentAI: AI? get() = teamToPlayer[currentTeam]

  var currentTeam: Team = Settings.startTeam
    internal set

  private val teamToPlayer =
    EnumMap<Team, AI?>(Team::class.java).apply {
      // if we create more teams this makes sure they are playing along
      putAll(Team.entries.map { it to Difficulty.HARD.aiConstructor(it) })

      put(SUN, Settings.teamSunAI.aiConstructor(SUN))
      put(LEAF, Settings.teamLeafAI.aiConstructor(LEAF))
      put(FOREST, Settings.teamForestAI.aiConstructor(FOREST))
      put(EARTH, Settings.teamEarthAI.aiConstructor(EARTH))
      put(STONE, Settings.teamStoneAI.aiConstructor(STONE))
    }

  private var aiJob: Job? = null

  init {
    restoreState(width, height, layout, territoryCoordinate, handCoordinate, handPiece, handRestoreAction, hexagonData, startTeam)
    history.clear()
  }

  fun isTeamAI(team: Team) = teamToPlayer[team] != null
  fun isTeamHuman(team: Team) = teamToPlayer[team] == null
  fun isCurrentTeamAI() = currentAI != null
  fun isCurrentTeamHuman() = currentAI == null

  fun updateHexagonVisibility(data: HexagonData) {
    val disabled = data.isDisabled
    if (disabled) {
      val hex = internalVisibleHexagons.find { getData(it) === data } ?: error("Failed to find this data! $data")
      internalVisibleHexagons -= hex
    } else {
      val hex = allHexagons.find { getData(it) === data } ?: error("Failed to find this data! $data")
      internalVisibleHexagons += hex
    }
    Events.fireEvent(HexagonVisibilityChanged(data, disabled))
  }

  // ////////// //
  //  Gameplay  //
  // ////////// //

  private fun recountHexagons(hexagonsPerTeamMap: EnumMap<Team, Int> = hexagonsPerTeam) {
    hexagonsPerTeamMap.clear()
    visibleHexagons.withData(this) { _, data ->
      hexagonsPerTeamMap.compute(data.team) { _, old -> 1 + (old ?: 0) }
    }
  }

  fun calculatePercentagesHexagons(): EnumMap<Team, Float> {
    val totalHexagons = visibleHexagons.count().toFloat().coerceAtLeast(1f)
    return Team.entries.toTypedArray().associateWithTo(EnumMap<Team, Float>(Team::class.java)) {
      val current = hexagonsPerTeam.getOrDefault(it, 0)
      current / totalHexagons
    }
  }

  fun endTurn() {
    aiJob = KtxAsync.launch(Hex.asyncThread) {
      select(null)

      if (checkGameEnded()) {
        gameInteraction.endGame()
        return@launch
      }

      val oldTeam = currentTeam
      val newTeam = Team.entries.next(oldTeam)
      currentTeam = newTeam
      Gdx.app.debug("TURN") { "Starting turn of $newTeam" }
      Gdx.app.postRunnable { Events.fireEvent(TeamEndTurnEvent(oldTeam, newTeam)) }

      if (newTeam == Settings.startTeam) {
        Gdx.app.debug("TURN", "New round!")
        round++
      }

      val capitals = mutableSetOf<Hexagon<HexagonData>>()
      visibleHexagons.withData(this@Island) { hexagon, data ->
        Gdx.app.trace("TURN") { "Handling new turn of ${data.piece::class.simpleName} (${hexagon.gridX},${hexagon.gridZ})" }
        data.piece.newTurn(this@Island, hexagon, data)
        if (data.team == newTeam) {
          if (data.piece is Capital) {
            capitals.add(hexagon)
          } else {
            Gdx.app.trace("TURN") { "Handling begin turn of ${data.piece::class.simpleName} (${hexagon.gridX},${hexagon.gridZ})" }
            data.piece.beginTeamTurn(this@Island, hexagon, data, newTeam)
          }
        }
      }

      // Process capitals last to ensure any pieces killed by bankruptcy
      // will not be turns into trees during the same turn
      for (capital in capitals) {
        val data = this@Island.getData(capital)
        Gdx.app.trace("TURN") { "Handling begin turn of ${data.piece::class.simpleName} (${capital.gridX},${capital.gridZ})" }
        data.piece.beginTeamTurn(this@Island, capital, data, newTeam)
      }
      Gdx.graphics.requestRendering()

      if (capitals.isNotEmpty()) {
        beginTurn()
      } else {
        if (isTeamHuman(newTeam) && singleAliveRealPlayer) {
          gameInteraction.endGame(false)
        } else {
          Gdx.app.debug("TURN", "Team $newTeam have no territories, skipping their turn")
          endTurn()
        }
      }
    }
  }

  fun beginTurn() {
    KtxAsync.launch(Hex.asyncThread) {
      history.clear()
      val cai = currentAI
      if (cai != null) {
        history.disable()
        val time = measureTimeMillis {
          try {
            cai.action(this@Island, gameInteraction)
          } catch (cancel: CancellationException) {
            // Ignore cancel exceptions
          } catch (e: RuntimeException) {
            e.printStackTrace()
            MessagesRenderer.publishError("Exception thrown during AI turn: ${e::class.simpleName} ${e.message}", Float.MAX_VALUE, e)
          }
        }
        Gdx.app.debug("AI") { "${cai.team} AI's turn took $time ms" }
        if (Hex.screen is PreviewIslandScreen) {
          endTurn()
        }
      } else {
        // enable history only when it's a humans turn
        history.enable()

        // Loose when no player have any capitals left
        if (visibleHexagons.none { getData(it).let { data -> teamToPlayer[data.team] == null && data.piece is Capital } }
        ) {
          gameInteraction.endGame(false)
        }
      }
    }
  }

  fun cancelCurrentAI() {
    try {
      aiJob?.cancel()
    } catch (ignore: Exception) {
    } finally {
      aiJob = null
    }
  }

  /**
   * Select the territory which [hexagon] is a part of, given the hexagon is a part of a territory and is the [currentTeam]
   *
   * @return if the selection was successful
   * */
  fun select(hexagon: Hexagon<HexagonData>?): Boolean {
    val oldSelected = selected

    history.ignore {
      hand = null
      selected = null
    }

    Gdx.app.trace("SELECT") { "Selecting hexagon ${hexagon?.coordinates}" }

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
  fun findTerritory(maybeHex: Hexagon<HexagonData>?): Territory? {
    val startTurn = turn
    val hexagon = maybeHex ?: return null
    val territoryHexes = getTerritoryHexagons(hexagon) ?: return null
    val capitalHex = findOrCreateCapital(territoryHexes) ?: return null
    val capital = getData(capitalHex).piece as? Capital ?: return null
    if (turn != startTurn) return null
    return Territory(this, capital, territoryHexes, capitalHex)
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
  fun findOrCreateCapital(territoryHexes: Collection<Hexagon<HexagonData>>): Hexagon<HexagonData>? {
    val capitals = territoryHexes.filter { getData(it).piece is Capital }
    if (capitals.isEmpty()) {
      // No capital found, generate a new capital
      val hexagon = calculateBestCapitalPlacement(territoryHexes)
      val capHexData = getData(hexagon)

      return if (capHexData.setPiece<Capital>()) {
        hexagon
      } else {
        Gdx.app.error("Island", "Failed to set capital at ${hexagon.coordinates}, there is a ${capHexData.piece} there")
        null
      }
    } else if (capitals.size == 1) {
      return capitals.first()
    }
    // No capital found, generate a new capital

    // there might be multiple capitals in the set of hexagons.
    // Find the best one, transfer all assets and delete the others

    val bestCapitalHex = calculateBestCapitalPlacement(capitals)
    val bestData = getData(bestCapitalHex).piece as? Capital ?: return null
    for (capital in capitals) {
      if (capital === bestCapitalHex) continue
      val data = getData(capital)
      val otherCapital = data.piece as? Capital
      otherCapital?.transfer(bestData)
      data.setPiece<Empty>()
    }
    return bestCapitalHex
  }

  /**
   * Get all hexagons that is in the same territory as the given [hexagon] or
   * `null` if hexagon is not a part of a territory
   */
  fun getTerritoryHexagons(hexagon: Hexagon<HexagonData>): Set<Hexagon<HexagonData>>? {
    val territoryHexes = connectedTerritoryHexagons(hexagon)
    if (territoryHexes.size < MIN_HEX_IN_TERRITORY) {
      if (Hex.mapEditor) {
        // When in map editor, we do not want to adhere to game logic. It will be validated before saving
        return null
      }

      // If given hexagon is a capital, but it is no longer a part of a territory (ie it's on its own)
      // then replace the capital with a tree
      val data = getData(hexagon)
      if (data.piece is Capital) {
        replaceWithTree(this, hexagon)
      } else if (data.piece is LivingPiece) {
        data.setPiece<Grave>()
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
  private fun calculateBestCapitalPlacement(hexagons: Collection<Hexagon<HexagonData>>): Hexagon<HexagonData> {
    val hexTeam = getData(hexagons.first()).team
    require(hexagons.all { getData(it).team == hexTeam }) { "All hexagons given must be on the same team" }

    // Capitals should prefer a worse location in favor of overwriting another piece
    val maxPlacementPreference = hexagons.minOf { getData(it).piece.capitalReplacementResistance }
    val feasibleHexagons = hexagons.filter { getData(it).piece.capitalReplacementResistance <= maxPlacementPreference }

    // The maximum distance between two hexagons for this grid
    val maxRadius = 3 * max(grid.gridData.gridWidth, grid.gridData.gridHeight) + 1

    fun findDistanceToClosestEnemyHex(hex: Hexagon<HexagonData>): Int {
      for (radius in 1..maxRadius) {
        val isAnyEnemies = calculateRing(hex, radius).any {
          val data = getData(it)
          data.team != hexTeam && data.visible && it.isPartOfATerritory(this)
        }
        if (isAnyEnemies) {
          return radius
        }
      }
      return NO_ENEMY_HEXAGONS
    }

    val contenders: List<Hexagon<HexagonData>> = if (Hex.mapEditor) {
      // During initial capital placement, it's more important to place capitals to cover maximum friendly area
      // As there are few situations where a capital far away from enemies is better than one close to friendly hexagons
      feasibleHexagons
    } else {
      feasibleHexagons.associateWith { findDistanceToClosestEnemyHex(it) }.let { allContenders ->
        val maxDistance: Int = allContenders.values.max()
        allContenders.filterValues { distance -> distance == maxDistance }.map { it.key }
      }
    }

    require(contenders.isNotEmpty()) {
      "No capital contenders found in ${hexagons.map { "@${it.coordinates} | data ${getData(it)}" }}"
    }

    Gdx.app.trace("ISLAND") {
      contenders.joinToString(prefix = "Contenders are \n\t", separator = "\n\t") {
        "${getData(it)}@${it.coordinates} dist of ${findDistanceToClosestEnemyHex(it)}"
      }
    }

    if (contenders.size == 1) return contenders.first()

    // if we have multiple contenders to become the capital, select the one with the fewest enemy hexagons near it.
    // Invisible hexagons count as ours hexagons

    var currBest: Hexagon<HexagonData>? = null
    var currBestScore = -1.0

    for (origin in contenders) {
      val ring = getNeighbors(origin, onlyVisible = false)
      val calcScore: Double = ring.sumOf {
        val data = getData(it)
        val score = when {
          data.visible && data.team == hexTeam ->
            when {
              // Prefer to place capitals next to castles, as they provide more defense
              // This will almost always happen during gameplay, and a placed castle hints at a good location for a capital
              data.piece is Castle -> 3.0
              // Normal defended friendly hexagons
              // Note: We do not consider living pieces as they can and will be moved around
              else -> return@sumOf 1.0
            }
          // An invisible hexagon cannot become enemy territories, and are thus always safe
          // Note: a visible friendly hexagon is better than an invisible hexagon,
          //       so the boost should be so small it only matters when there is a tie
          data.invisible -> 0.5
          // A small boost for hexagons which are not part of a territory
          // Note: a visible friendly hexagon is better than an invisible hexagon,
          //       so the boost should be so small it only matters when there is a tie
          data.visible && data.team != hexTeam && !it.isPartOfATerritory(this) -> 0.1
          else -> 0.0
        }
        Gdx.app.trace("ISLAND") { "\t${origin.coordinates}: +$score from ${it.coordinates}" }
        score
      }
      Gdx.app.trace("ISLAND") {
        val candidateInfo = "capital candidate found at ${origin.coordinates} (${getData(origin)}) with score $calcScore"
        if (calcScore > currBestScore) {
          "New best $candidateInfo"
        } else {
          "         $candidateInfo"
        }
      }
      if (calcScore > currBestScore) {
        currBestScore = calcScore
        currBest = origin
      }
    }

    Gdx.app.trace("ISLAND") { "Best capital found was ${getData(currBest!!)}@${currBest.coordinates} with a score of $currBestScore" }

    return currBest ?: error("No best!?")
  }

  /**
   * The game has ended when there is only one team left with a capital left
   */
  fun checkGameEnded(): Boolean {
    val currentTeam = currentTeam
    forEachPieceType<Capital> { _, data, _ ->
      if (data.team != currentTeam) {
        // Someone else has a capital, the game has not ended yet!
        return false
      }
    }
    return true
  }

  // /////////////////
  // Serialization  //
  // /////////////////

  /**
   *
   * Validation rules:
   *
   * * All visible hexagons must be reachable from all other visible hexagons (ie there can only be
   * one island)
   * * No capital pieces in territories with size smaller than [MIN_HEX_IN_TERRITORY]
   * * There must be exactly one capital per territory
   * * Each team must have at least one capital
   *
   * @return If this island is valid.
   */
  fun validate(): Boolean {
    val violations = mutableListOf<String>()

    fun registerViolation(message: String) {
      violations += message
    }

    // Check rules which apply to whole territories
    val territoryHexagons = HashSet<Hexagon<HexagonData>>()
    val totalCapitalCount = EnumMap<Team, Int>(Team::class.java)
    for (hexagon in visibleHexagons) {
      if (territoryHexagons.contains(hexagon)) continue

      val team = this.getData(hexagon).team
      val connectedHexes = connectedTerritoryHexagons(hexagon, team)
      territoryHexagons.addAll(connectedHexes)

      if (connectedHexes.size < MIN_HEX_IN_TERRITORY) {
        for (invalidTerrHex in connectedHexes) {
          if (getData(invalidTerrHex).piece is Capital) {
            registerViolation("Hexagon ${invalidTerrHex.coordinates} is a capital, even though its territory has fewer than $MIN_HEX_IN_TERRITORY hexagons in it.")
          }
        }
        continue
      }

      val capitalCount = connectedHexes.count { getData(it).piece is Capital }
      if (capitalCount < 1) {
        registerViolation("There exists a territory with no capital. Hexagon ${hexagon.coordinates} is within it.")
      } else if (capitalCount > 1) {
        registerViolation("There exists a territory with more than one capital. Hexagon ${hexagon.coordinates} is within it.")
      }

      totalCapitalCount[team] = totalCapitalCount.getOrDefault(team, 0) + capitalCount
    }

    for (team in Team.entries) {
      val count = totalCapitalCount.getOrDefault(team, 0)
      if (count < 1) {
        registerViolation("There are no capitals for team $team, every team must have at least one capital")
      }
    }

    // Check rules which apply to each visible hexagon
    // Done because hexagons are skipped in the territory check above
    for (hexagon in visibleHexagons) {
      val data = this.getData(hexagon)
      val team = data.team
      val piece = data.piece
      // Check rules which apply to each visible hexagon
      if (piece is TreePiece) {
        if (piece::class != idealTreeType(hexagon)) {
          registerViolation(
            "Hexagon ${hexagon.coordinates} is the wrong tree type, it is a ${piece::class.simpleName} but should be a ${idealTreeType(hexagon).simpleName}"
          )
        }
        if (piece.lastGrownTurn != team.ordinal) {
          registerViolation(
            "Hexagon ${hexagon.coordinates} has a tree that was last grown on turn ${piece.lastGrownTurn}, but it should be ${team.ordinal} (the team's ordinal)"
          )
        }
      }
    }

    // check that every hexagon is connected
    val visibleNeighbors = HashSet<Hexagon<HexagonData>>(visibleHexagons.size)
    val toCheck = Queue<Hexagon<HexagonData>>(visibleHexagons.size * 2)
    val firstVisible = visibleHexagons.firstOrNull()
    if (firstVisible == null) {
      registerViolation("There are no visible hexagons")
    } else {
      toCheck.addFirst(firstVisible)
    }

    while (!toCheck.isEmpty) {
      val curr: Hexagon<HexagonData> = toCheck.removeFirst()
      if (curr in visibleNeighbors) continue
      val neighbors = getNeighbors(curr)

      for (neighbor in neighbors) {
        if (neighbor in visibleNeighbors) continue
        toCheck.addLast(neighbor)
      }
      visibleNeighbors += curr
    }

    if (!visibleHexagons.containsAll(visibleNeighbors) || !visibleNeighbors.containsAll(visibleHexagons)) {
      registerViolation("The visible hexagon grid is not connected.")
    }

    // Check rules which apply to each invisible hexagon
    for (hexagon in invisibleHexagons) {
      val data = getData(hexagon)
      if (data.piece !is Empty) {
        registerViolation("Hexagon ${hexagon.coordinates} is invisible but has a piece on it! ${data.piece}")
      }
    }

    if (violations.isNotEmpty()) {
      val durationSeconds = violations.size * 10f
      MessagesRenderer.publishError("Island validation failed with ${violations.size} violations:", durationSeconds)
      for (violation in violations) {
        MessagesRenderer.publishError(violation, durationSeconds)
      }
    }
    return violations.isEmpty()
  }

  companion object {
    const val GRID_RADIUS = 20.0

    const val MIN_HEX_IN_TERRITORY = 2

    const val START_CAPITAL_PER_HEX = 5
    const val MAX_START_CAPITAL = 25

    const val NO_ENEMY_HEXAGONS = -1

    /**
     * The map have never been played, so we don't know how hard it is
     */
    const val NEVER_PLAYED = 0

    /**
     * The map have been played but never beaten, i.e., it might be impossible/extremely hard to beat
     */
    const val NEVER_BEATEN = 999

    fun Hand.createDtoPieceCopy(): Piece = piece.let { it.copyTo(if (restore != NoRestore) it.data.copy() else EDGE_DATA) }

    fun deserialize(json: String): Island = Hex.mapper.readValue(json)
    fun deserialize(file: FileHandle): Island = deserialize(file.readString())
  }

  // ////////////////////////
  // Data Transfer Object //
  // ////////////////////////

  @JsonValue
  fun createDto(): IslandDto {
    val hand = hand
    val handPiece = hand?.createDtoPieceCopy()
    val handRestoreAction = Hand.Companion.RestoreAction.toString(hand?.restore)
    val handCoordinate = hand?.territory?.hexagons?.find { getData(it) === hand.piece.data }?.cubeCoordinate
    val territoryCoord = selected?.hexagons?.first()?.cubeCoordinate

    return IslandDto(
      width = grid.gridData.gridWidth,
      height = grid.gridData.gridHeight,
      layout = grid.gridData.gridLayout.toEnumValue(),
      territoryCoordinate = territoryCoord,
      handCoordinate = handCoordinate,
      handPiece = handPiece,
      handRestoreAction = handRestoreAction,
      hexagonData = hexagonCoordinateToData.toSortedMap(),
      round = round,
      team = currentTeam
    )
  }
}