package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Keys.BACK
import com.badlogic.gdx.Input.Keys.ESCAPE
import com.badlogic.gdx.math.Vector2
import no.elg.hex.Hex
import no.elg.hex.Settings
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.platform.PlatformType
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.getHexagon
import no.elg.hex.util.playClick
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

  private var pinching = false

  private val lastPointer1 = Vector2()
  private val lastPointer2 = Vector2()
  private val currentPointer1 = Vector2()
  private val currentPointer2 = Vector2()

  val cursorHex: Hexagon<HexagonData>? get() = screen.island.getHexagon(mouseX.toDouble(), mouseY.toDouble())

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    if (pointer == 0 && isCorrectButtonPressed()) {
      val zoom = screen.camera.zoom
      val dx = -Gdx.input.deltaX * zoom
      val dy = -Gdx.input.deltaY * zoom

      // Make the little movements when clicking fast less noticeable
      if (abs(dx) < MIN_MOVE_AMOUNT * zoom && abs(dy) < MIN_MOVE_AMOUNT * zoom) {
        return false
      }

      screen.camera.translate(dx, dy)
      screen.enforceCameraBounds()
      return false
    }
    return false
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      ESCAPE, BACK -> {
        Hex.screen = LevelSelectScreen()
        playClick()
      }

      else -> return false
    }
    return true
  }

  private fun getTargetZoom(amount: Float): Float? {
    val zoom = amount * (screen.camera.zoom / 3f).coerceAtMost(1f) + screen.camera.zoom
    if (zoom.isNaN()) {
      Gdx.app.error("Island Zoom", "Tried to update zoom to NaN!")
      return null
    }
    return zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
  }

  private fun instantlyZoom(amount: Float) {
    getTargetZoom(amount)?.also {
      screen.smoothTransition = null
      screen.camera.zoom = it
    }
  }

  private fun smoothZoom(amount: Float) {
    getTargetZoom(amount)?.also { zoom ->
      updateMouse()
      screen.smoothTransition = SmoothTransition(screen, zoom, mouseX, mouseY, 0.25f)
    }
  }

  override fun scrolled(amountX: Float, amountY: Float): Boolean {
    instantlyZoom(amountY * Settings.zoomSpeed)
    screen.camera.update()
    return true
  }

  private fun unprojectVector(src: Vector2, destination: Vector2) {
    destination.set(src.x / Gdx.graphics.ppcX, src.y / Gdx.graphics.ppcY)
  }

  override fun pinch(initialPointer1: Vector2, initialPointer2: Vector2, pointer1: Vector2, pointer2: Vector2): Boolean {
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

    instantlyZoom(amount)
    screen.camera.update()

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

  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
    if (Settings.enableDoubleTapToZoom && count % 2 == 0 && screen.smoothTransition == null && isCorrectButtonPressed()) {
      smoothZoom(TAP_ZOOM_AMOUNT)
      return true
    }
    return false
  }

  companion object {
    private const val MIN_MOVE_AMOUNT = 0

    const val MIN_ZOOM = 0.1f
    const val MAX_ZOOM = 2f

    const val TAP_ZOOM_AMOUNT = -1.5f

    fun isCorrectButtonPressed(): Boolean =
      when (Hex.platform.type) {
        PlatformType.MOBILE -> true
        PlatformType.DESKTOP -> Gdx.input.isButtonPressed(Input.Buttons.RIGHT) && !Gdx.input.isButtonPressed(Input.Buttons.LEFT)
      }
  }
}