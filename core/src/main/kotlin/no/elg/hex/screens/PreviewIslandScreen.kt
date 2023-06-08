package no.elg.hex.screens

import no.elg.hex.Hex
import no.elg.hex.event.HexagonChangedTeamEvent
import no.elg.hex.event.HexagonVisibilityChanged
import no.elg.hex.event.SimpleEventListener
import no.elg.hex.input.BasicIslandInputProcessor
import no.elg.hex.input.BasicIslandInputProcessor.Companion.MAX_ZOOM
import no.elg.hex.input.BasicIslandInputProcessor.Companion.MIN_ZOOM
import no.elg.hex.input.SmoothTransition
import no.elg.hex.island.Island
import no.elg.hex.renderer.OutlineRenderer
import no.elg.hex.renderer.SpriteRenderer
import no.elg.hex.renderer.StrengthBarRenderer
import no.elg.hex.renderer.VerticesRenderer
import no.elg.hex.screens.SplashIslandScreen.Companion.createIslandScreen
import no.elg.hex.util.VisibleGrid.Companion.calculateVisibleGrid
import no.elg.hex.util.isLazyInitialized
import kotlin.math.max

/** @author Elg */
open class PreviewIslandScreen(val id: Int, val island: Island, private val isPreviewRenderer: Boolean) : AbstractScreen() {

  val basicIslandInputProcessor by lazy { BasicIslandInputProcessor(this) }

  var smoothTransition: SmoothTransition? = null

  private val visibleGridSize get() = if (Hex.args.mapEditor) { island.calculateVisibleGrid() } else { lazyVisibleGridSize }
  private val lazyVisibleGridSize by lazy { island.calculateVisibleGrid() }
  private val verticesRenderer by lazy { VerticesRenderer(this) }
  private val outlineRenderer by lazy { OutlineRenderer(this) }
  private val spriteRenderer by lazy { SpriteRenderer(this) }
  private val strengthBarRenderer by lazy { StrengthBarRenderer(this.island) }
  private lateinit var teamChangedListener: SimpleEventListener<HexagonChangedTeamEvent>
  private lateinit var visibilityChangedListener: SimpleEventListener<HexagonVisibilityChanged>

  var smoothTransition: SmoothTransition? = null

  override fun render(delta: Float) {
    verticesRenderer.frameUpdate()
    outlineRenderer.frameUpdate()
    spriteRenderer.frameUpdate()

    if (!isPreviewRenderer) {
      if (StrengthBarRenderer.isEnabled) {
        strengthBarRenderer.frameUpdate()
      }
      smoothTransition?.zoom(delta)?.also { done ->
        if (done) {
          smoothTransition = null
        }
      }
    }
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    val data = island.grid.gridData
    if (island.allHexagons.isEmpty()) return

    val (maxX, minX, maxY, minY, maxInvX, maxInvY) = if (Hex.args.mapEditor) {
      calcVisibleGridSize()
    } else {
      visibleGridSize
    }

    if (StrengthBarRenderer.isEnabled) {
      strengthBarRenderer.resize(width, height)
    }

    // Sum the distance from the edge of the grid to the first visible hexagon
    // |.###..| (`.` are invisible, `#` are visible hexagons)
    // The offset would then be 3
    val gridWidthOffset = maxInvX - minX - maxX
    val gridHeightOffset = maxInvY - minY - maxY

    val islandCenterX = (maxInvX - gridWidthOffset) / 2
    val islandCenterY = (maxInvY - gridHeightOffset) / 2

    // Add some padding as the min/max x/y are calculated from the center of the hexagons
    val padding = 2
    val widthZoom = (maxX - minX + padding * data.hexagonWidth) / camera.viewportWidth
    val heightZoom = (maxY - minY + padding * data.hexagonHeight) / camera.viewportHeight

    camera.position.x = islandCenterX.toFloat()
    camera.position.y = islandCenterY.toFloat()
    camera.zoom = max(widthZoom, heightZoom).toFloat()
    updateCamera()
  }

  fun enforceCameraBounds(updateCamera: Boolean = true) {
    val (maxX, minX, maxY, minY) = visibleGridSize
    camera.position.x = camera.position.x.coerceIn(minX.toFloat(), maxX.toFloat())
    camera.position.y = camera.position.y.coerceIn(minY.toFloat(), maxY.toFloat())
    camera.zoom = camera.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    if (updateCamera) {
      camera.update()
      Gdx.graphics.requestRendering()
    }
  }
  override fun show() {
    basicIslandInputProcessor.show()

    if (!isPreviewRenderer) {
      teamChangedListener = SimpleEventListener.create {
        island.hexagonsPerTeam.compute(it.old) { _, old -> (old ?: 0) - 1 }
        island.hexagonsPerTeam.compute(it.new) { _, old -> (old ?: 0) + 1 }
      }
      visibilityChangedListener = SimpleEventListener.create {
        island.hexagonsPerTeam.compute(it.data.team) { _, old -> (old ?: 0) + if (it.isDisabled) -1 else 1 }
      }
    }
  }

  override fun dispose() {
    island.select(null)

    super.dispose()
    if (::verticesRenderer.isLazyInitialized) {
      verticesRenderer.dispose()
    }
    if (::outlineRenderer.isLazyInitialized) {
      outlineRenderer.dispose()
    }
    if (::spriteRenderer.isLazyInitialized) {
      spriteRenderer.dispose()
    }
    if (::strengthBarRenderer.isLazyInitialized) {
      strengthBarRenderer.dispose()
    }
    if (::teamChangedListener.isInitialized) {
      teamChangedListener.dispose()
    }
    if (::visibilityChangedListener.isInitialized) {
      visibilityChangedListener.dispose()
    }
  }
}