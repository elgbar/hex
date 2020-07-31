package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector3
import no.elg.hex.hexagon.HexagonData
import no.elg.hex.screens.IslandScreen
import no.elg.hex.util.getHexagon
import org.hexworks.mixite.core.api.Hexagon
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

/**
 * Handles input event related to the camera and other basic functiuons such as what
 *
 * @author Elg
 */
class BasicInputProcessor(private val islandScreen: IslandScreen) : InputAdapter() {

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

  var saveSlot: Int = 0
    private set

  val cursorHex: Hexagon<HexagonData>? get() = islandScreen.island.getHexagon(mouseX.toDouble(), mouseY.toDouble())

  /**
   * Update the world mouse position.
   * Will only update if the frame has changed since last called
   */
  private fun updateMouse() {
    if (lastMouseFrame == Gdx.graphics.frameId) return
    unprojectVector.x = Gdx.input.x.toFloat()
    unprojectVector.y = Gdx.input.y.toFloat()

    islandScreen.camera.unproject(unprojectVector)
    mouseX = unprojectVector.x
    mouseY = unprojectVector.y
  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    if (draggable) {
      val zoom = islandScreen.camera.zoom
      val x: Float = Gdx.input.deltaX * zoom
      val y: Float = Gdx.input.deltaY * zoom

      //Make the little movements when clicking fast less noticeable
      if (abs(x) < MIN_MOVE_AMOUNT * zoom && abs(y) < MIN_MOVE_AMOUNT * zoom) {
        return false
      }

      islandScreen.camera.translate(-x, -y)
      return true
    }
    return false
  }

  override fun keyDown(keycode: Int): Boolean {
    when (keycode) {
      Keys.RIGHT -> saveSlot++
      Keys.LEFT -> saveSlot = max(saveSlot - 1, 0)
      else -> return false
    }
    return true
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

  fun resetCamera() {
    val data = islandScreen.island.grid.gridData

    val x = (data.gridWidth * data.hexagonWidth + data.gridWidth).toFloat() / 2f
    val y = (data.gridHeight * data.hexagonHeight + data.gridHeight).toFloat() / 2f

    islandScreen.camera.position.x = x
    islandScreen.camera.position.y = y
  }

  override fun scrolled(amount: Int): Boolean {
    islandScreen.camera.zoom = (sign(amount.toFloat()) * ZOOM_SPEED + islandScreen.camera.zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
    return true
  }

  companion object {
    private const val MIN_MOVE_AMOUNT = 0

    const val MIN_ZOOM = 0.15f
    const val MAX_ZOOM = 3.0f

    private const val ZOOM_SPEED = 0.1f

    private val unprojectVector = Vector3()
    private var draggable = false
    private var lastMouseFrame: Long = -1

  }
}
