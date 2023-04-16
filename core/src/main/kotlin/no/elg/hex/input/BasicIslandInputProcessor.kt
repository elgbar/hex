package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.BACK
import com.badlogic.gdx.Input.Keys.ESCAPE
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.getHexagon
import no.elg.hex.util.trace
import org.hexworks.mixite.core.api.Hexagon
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Handles input event related to the camera and other basic functiuons such as what
 *
 * @author Elg
 */
class BasicIslandInputProcessor(private val screen: PreviewIslandScreen) : AbstractInput(true) {

  var mouseX: Float = 0f
    private set
    get() {
      updateMouse()
      return field
    }
  var mouseY: Float = 0f
    private set
    get() {
      updateMouse()
      return field
    }

  private val unprojectVector = Vector3()
  private var draggable = false
  private var pinching = false
  private var lastMouseFrame: Long = -1

  private val lastPointer1 = Vector2()
  private val lastPointer2 = Vector2()
  private val currentPointer1 = Vector2()
  private val currentPointer2 = Vector2()

  val cursorHex: Hexagon<HexagonData>? get() = screen.island.getHexagon(mouseX.toDouble(), mouseY.toDouble())

  /**
   * Update the world mouse position. Will only update if the frame has changed since last called
   */
  private fun updateMouse() {
    if (lastMouseFrame == Gdx.graphics.frameId) return
    unprojectVector.x = Gdx.input.x.toFloat()
    unprojectVector.y = Gdx.input.y.toFloat()

    screen.camera.unproject(unprojectVector)
    mouseX = unprojectVector.x
    mouseY = unprojectVector.y
  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    if (draggable && pointer == 0) {
      val (maxX, minX, maxY, minY) = screen.visibleGridSize

      val zoom = screen.camera.zoom
      val dx = -Gdx.input.deltaX * zoom
      val dy = -Gdx.input.deltaY * zoom

      // Make the little movements when clicking fast less noticeable
      if (abs(dx) < MIN_MOVE_AMOUNT * zoom && abs(dy) < MIN_MOVE_AMOUNT * zoom) {
        return false
      }

      screen.camera.translate(dx, dy)
      screen.camera.position.x = screen.camera.position.x.coerceIn(minX.toFloat(), maxX.toFloat())
      screen.camera.position.y = screen.camera.position.y.coerceIn(minY.toFloat(), maxY.toFloat())
      screen.camera.update()
      return false
    }
    return false
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      ESCAPE, BACK -> {
        Hex.screen = LevelSelectScreen
        Hex.assets.clickSound?.play(Settings.volume)
      }

      else -> return false
    }
    return true
  }

  private fun updateZoom(amount: Float) {
    val zoom = amount * (screen.camera.zoom / 3f).coerceAtMost(1f) + screen.camera.zoom
    if (zoom.isNaN()) {
      Gdx.app.error("Island Zoom", "Tried to update zoom to NaN!")
      return
    }
    screen.camera.zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    screen.camera.update()
  }

  override fun scrolled(amountX: Float, amountY: Float): Boolean {
    updateZoom(amountY * Settings.zoomSpeed)
    return true
  }

  private fun unprojectVector(src: Vector2, destination: Vector2) {
    destination.set(src.x / Gdx.graphics.ppcX, src.y / Gdx.graphics.ppcY)
  }

  override fun pinch(
    initialPointer1: Vector2,
    initialPointer2: Vector2,
    pointer1: Vector2,
    pointer2: Vector2
  ): Boolean {
    if (!pinching) {
      Gdx.app.debug("pinch zoom", "Pinch start")
      unprojectVector(initialPointer1, lastPointer1)
      unprojectVector(initialPointer2, lastPointer2)
    }
    pinching = true
    unprojectVector(pointer1, currentPointer1)
    unprojectVector(pointer2, currentPointer2)

    // figure out which way we're zooming,
    // if the last distance was greater than the current distance the fingers are closer, we are zooming out
    // if the last distance was less than the current distance the fingers are further apart, we are zooming in
    val lastDistance = lastPointer1.dst2(lastPointer2)
    val currentDistance = currentPointer1.dst2(currentPointer2)
    val sign = if (lastDistance < currentDistance) -1 else 1

    // Select the distance zoomed which is the greatest
    val distance1 = currentPointer1.dst2(lastPointer1)
    val distance2 = currentPointer2.dst2(lastPointer2)
    val distance = sqrt(max(distance1, distance2))

    val amount = sign * distance

    Gdx.app.trace("pinch zoom") { "$lastPointer1 | $lastPointer2 | $currentPointer1 | $currentPointer2" }
    Gdx.app.trace("pinch zoom") { "Last distance $lastDistance | current distance $currentDistance | sign $sign -> we are zooming ${if (sign == 1) "in" else "out"}" }
    Gdx.app.trace("pinch zoom") { "$sign * $distance * ${Gdx.graphics.deltaTime} -> $amount" }

    updateZoom(amount)

    lastPointer1.set(currentPointer1)
    lastPointer2.set(currentPointer2)
    return true
  }

  override fun pinchStop() {
    pinching = false
    Gdx.app.debug("pinch zoom", "Pinch end")
    lastPointer1.set(0f, 0f)
    lastPointer2.set(0f, 0f)
  }

  override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
    draggable = true
    return false
  }

  override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean {
    draggable = false
    return false
  }

  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
    if (count > 1) {
      updateZoom(TAP_ZOOM_AMOUNT)
      return true
    }
    return false
  }

  companion object {
    private const val MIN_MOVE_AMOUNT = 0

    const val MIN_ZOOM = 0.1f
    const val MAX_ZOOM = 3.0f

    const val TAP_ZOOM_AMOUNT = -1.5f
  }
}