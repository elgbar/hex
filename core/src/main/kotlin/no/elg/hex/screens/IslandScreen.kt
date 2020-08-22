package no.elg.hex.screens

import com.badlogic.gdx.InputProcessor
import kotlin.math.max
import no.elg.hex.Hex
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.DebugInfoRenderer
import no.elg.hex.hud.GameInfoRenderer
import no.elg.hex.hud.MapEditorRenderer
import no.elg.hex.input.BasicIslandInputProcessor
import no.elg.hex.input.GameInputProcessor
import no.elg.hex.input.MapEditorInputProcessor
import no.elg.hex.island.Island
import no.elg.hex.island.Island.Companion.STARTING_TEAM
import no.elg.hex.renderer.OutlineRenderer
import no.elg.hex.renderer.SpriteRenderer
import no.elg.hex.renderer.VerticesRenderer
import no.elg.hex.util.component6
import no.elg.hex.util.component7
import no.elg.hex.util.component8
import no.elg.hex.util.getData

/** @author Elg */
class IslandScreen(val id: Int, val island: Island, private val renderHud: Boolean = true) :
    AbstractScreen() {

  private fun calcVisibleGridSize(): DoubleArray {
    val visible = island.hexagons.filterNot { island.getData(it).invisible }

    val minX = visible.minOf { it.externalBoundingBox.x }
    val maxX = visible.maxOf { it.externalBoundingBox.x + it.externalBoundingBox.width }

    val minY = visible.minOf { it.externalBoundingBox.y + it.externalBoundingBox.height }
    val maxY = visible.maxOf { it.externalBoundingBox.y }

    val minInvX = island.hexagons.minOf { it.externalBoundingBox.x }
    val maxInvX = island.hexagons.maxOf { it.externalBoundingBox.x + it.externalBoundingBox.width }

    val minInvY = island.hexagons.minOf { it.externalBoundingBox.y + it.externalBoundingBox.height }
    val maxInvY = island.hexagons.maxOf { it.externalBoundingBox.y }
    return doubleArrayOf(maxX, minX, maxY, minY, maxInvX, minInvX, maxInvY, minInvY)
  }

  private val visibleGridSize by lazy { calcVisibleGridSize() }

  val inputProcessor: InputProcessor by lazy {
    if (Hex.args.mapEditor) {
      MapEditorInputProcessor(this)
    } else {
      GameInputProcessor(this)
    }
  }

  private val frameUpdatable: FrameUpdatable by lazy {
    if (Hex.args.mapEditor) {
      MapEditorRenderer(this, inputProcessor as MapEditorInputProcessor)
    } else {
      GameInfoRenderer(this, inputProcessor as GameInputProcessor)
    }
  }

  val basicIslandInputProcessor: BasicIslandInputProcessor by lazy {
    BasicIslandInputProcessor(this)
  }
  private val debugRenderer: DebugInfoRenderer by lazy { DebugInfoRenderer(this) }

  private val verticesRenderer = VerticesRenderer(this)
  private val outlineRenderer = OutlineRenderer(this)
  private val spriteRenderer = SpriteRenderer(this)

  override fun render(delta: Float) {
    camera.update()

    verticesRenderer.frameUpdate()
    outlineRenderer.frameUpdate()
    spriteRenderer.frameUpdate()

    if (renderHud) {
      if (Hex.debug) {
        debugRenderer.frameUpdate()
      }
      frameUpdatable.frameUpdate()
    }
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    val data = island.grid.gridData

    val (maxX, minX, maxY, minY, maxInvX, minInvX, maxInvY, minInvY) = if (Hex.args.mapEditor)
      calcVisibleGridSize()
    else visibleGridSize

    // Sum the distance from the edge of the grid to the first visible hexagon
    // |.###..| (`.` are invisible, `#` are visible hexagons)
    // The offset would then be 3
    val gridWidthOffset = minInvX - minX + maxInvX - maxX
    val gridHeightOffset = minInvY - minY + maxInvY - maxY

    val islandCenterX = (maxInvX - minInvX - gridWidthOffset) / 2
    val islandCenterY = (maxInvY - minInvY - gridHeightOffset) / 2


    // Add some padding as the min/max x/y are calculated from the center of the hexagons
    val padding = 2
    val widthZoom = (maxX - minX + padding * data.hexagonWidth) / camera.viewportWidth
    val heightZoom = (maxY - minY + padding * data.hexagonHeight) / camera.viewportHeight

    camera.position.x = islandCenterX.toFloat()
    camera.position.y = islandCenterY.toFloat()
    camera.zoom = max(widthZoom, heightZoom).toFloat()
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.addProcessor(inputProcessor)

    if (island.currentTeam != STARTING_TEAM && inputProcessor is GameInputProcessor) {
      island.endTurn(inputProcessor as GameInputProcessor)
    }
  }

  override fun hide() {
    dispose()
    island.select(null)
  }

  override fun dispose() {
    super.dispose()
    Hex.inputMultiplexer.removeProcessor(basicIslandInputProcessor)
    Hex.inputMultiplexer.removeProcessor(inputProcessor)
  }
}
