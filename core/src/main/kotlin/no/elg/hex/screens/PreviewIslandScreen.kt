package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import no.elg.hex.Hex
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.input.BasicIslandInputProcessor
import no.elg.hex.input.BasicIslandInputProcessor.Companion.MAX_ZOOM
import no.elg.hex.input.BasicIslandInputProcessor.Companion.MIN_ZOOM
import no.elg.hex.island.Island
import no.elg.hex.renderer.OutlineRenderer
import no.elg.hex.renderer.SpriteRenderer
import no.elg.hex.renderer.VerticesRenderer
import no.elg.hex.util.component6
import no.elg.hex.util.getData
import no.elg.hex.util.isLazyInitialized
import no.elg.hex.util.serialize
import org.hexworks.mixite.core.api.Hexagon
import kotlin.math.max

/** @author Elg */
open class PreviewIslandScreen(val id: Int, val island: Island) : AbstractScreen() {

  val basicIslandInputProcessor by lazy { BasicIslandInputProcessor(this) }

  private fun calcVisibleGridSize(): DoubleArray {
    val visible = island.hexagons.filterNot { island.getData(it).invisible }
    if (visible.isEmpty()) return doubleArrayOf(.0, .0, .0, .0, .0, .0)

    val minX = visible.minOf { it.externalBoundingBox.x }
    val maxX = visible.maxOf { it.externalBoundingBox.x + it.externalBoundingBox.width }

    val minY = visible.minOf { it.externalBoundingBox.y + it.externalBoundingBox.height }
    val maxY = visible.maxOf { it.externalBoundingBox.y }

    val maxInvX = island.hexagons.maxOf { it.externalBoundingBox.x + it.externalBoundingBox.width }
    val maxInvY = island.hexagons.maxOf { it.externalBoundingBox.y }

    return doubleArrayOf(maxX, minX, maxY, minY, maxInvX, maxInvY)
  }

  val visibleGridSize by lazy { calcVisibleGridSize() }

  private val verticesRenderer by lazy { VerticesRenderer(this) }
  private val outlineRenderer by lazy { OutlineRenderer(this) }
  private val spriteRenderer by lazy { SpriteRenderer(this) }

  val cursorHexagon: Hexagon<HexagonData>?
    get() = null

  override fun render(delta: Float) {
    verticesRenderer.frameUpdate()
    outlineRenderer.frameUpdate()
    spriteRenderer.frameUpdate()
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    val data = island.grid.gridData
    if (island.hexagons.isEmpty()) return

    val (maxX, minX, maxY, minY, maxInvX, maxInvY) = if (Hex.args.mapEditor) {
      calcVisibleGridSize()
    } else {
      visibleGridSize
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
    camera.zoom = max(widthZoom, heightZoom).toFloat().coerceIn(MIN_ZOOM, MAX_ZOOM)
    updateCamera()
  }

  override fun show() {
    basicIslandInputProcessor.show()
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
  }

  fun saveProgress() {
    Gdx.app.debug("IS PROGRESS", "Saving progress of island $id")
    islandPreferences.putString(getPrefName(id, false), island.createDto().serialize())
    islandPreferences.flush()
  }

  fun clearProgress() {
    Gdx.app.debug("IS PROGRESS", "Clearing progress of island $id")
    islandPreferences.remove(getPrefName(id, false))
    islandPreferences.remove(getPrefName(id, true))
    islandPreferences.flush()
  }

  companion object {
    val islandPreferences: Preferences by lazy { Gdx.app.getPreferences("island") }

    fun getProgress(id: Int, preview: Boolean = false): String? {
      val pref = getPrefName(id, preview)
      return islandPreferences.getString(pref, null)
    }

    fun getPrefName(id: Int, preview: Boolean) = "$id${if (preview) "-preview" else ""}"
  }
}