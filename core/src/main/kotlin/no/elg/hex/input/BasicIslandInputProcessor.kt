package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys.BACK
import com.badlogic.gdx.Input.Keys.ESCAPE
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector3
import no.elg.hex.Hex
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.screens.LevelSelectScreen
import no.elg.hex.screens.PreviewIslandScreen
import no.elg.hex.util.getHexagon
import org.hexworks.mixite.core.api.Hexagon
import kotlin.math.abs

/**
 * Handles input event related to the camera and other basic functiuons such as what
 *
 * @author Elg
 */
class BasicIslandInputProcessor(private val screen: PreviewIslandScreen) : InputAdapter() {

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
  private var lastMouseFrame: Long = -1

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
    if (draggable) {

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
      return true
    }
    return false
  }

  override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    when (button) {
      Buttons.RIGHT -> draggable = true
      else -> return false
    }
    return true
  }

  override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
    when (button) {
      Buttons.RIGHT -> draggable = false
      else -> return false
    }
    return true
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      ESCAPE, BACK -> Hex.screen = LevelSelectScreen
      else -> return false
    }
    return true
  }

  override fun scrolled(amountX: Float, amountY: Float): Boolean {
    screen.camera.zoom = (amountY * ZOOM_SPEED * (screen.camera.zoom / 3f).coerceAtMost(1f) + screen.camera.zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
    screen.camera.update()
    return true
  }

  companion object {
    private const val MIN_MOVE_AMOUNT = 0

    const val MIN_ZOOM = 0.1f
    const val MAX_ZOOM = 3.0f

    private const val ZOOM_SPEED = 0.1f
  }
}
