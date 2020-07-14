package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector3
import no.elg.hex.Hex
import no.elg.hex.Hex.camera
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hexagon.HexUtil
import no.elg.hex.hexagon.HexagonData
import org.hexworks.mixite.core.api.Hexagon
import java.awt.Toolkit
import kotlin.math.abs
import kotlin.math.sign

/**
 * @author Elg
 */
object InputHandler : InputAdapter(), FrameUpdatable {

  private const val ZOOM_ENABLE = false

  private const val MIN_MOVE_AMOUNT = 0

  const val MIN_ZOOM = 0.15f
  const val MAX_ZOOM = 3.0f

  private const val ZOOM_SPEED = 0.1f

  val scale: Int = if (Toolkit.getDefaultToolkit().screenSize.width > 2560) 2 else 1

  private val unprojectVector = Vector3()

  var mouseX: Float = 0f
    private set
  var mouseY: Float = 0f
    private set

  val cursorHex: Hexagon<HexagonData>? get() = HexUtil.getHexagon(mouseX.toDouble(), mouseY.toDouble())

  override fun frameUpdate() {
    unprojectVector.x = Gdx.input.x.toFloat()
    unprojectVector.y = Gdx.input.y.toFloat()

    camera.unproject(unprojectVector)
    mouseX = unprojectVector.x
    mouseY = unprojectVector.y

  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    val zoom = camera.zoom
    val x: Float = Gdx.input.deltaX * zoom
    val y: Float = Gdx.input.deltaY * zoom

    //Make the little movements when clicking fast less noticeable
    if (abs(x) < MIN_MOVE_AMOUNT * zoom && abs(y) < MIN_MOVE_AMOUNT * zoom) {
      return false
    }

    camera.translate(-x, -y)
    return true
  }

  fun resetCamera() {
    val data = Hex.map.grid.gridData

    val x = (data.gridWidth * data.hexagonWidth + data.gridWidth).toFloat() / 2f
    val y = (data.gridHeight * data.hexagonHeight + data.gridHeight).toFloat() / 2f

    camera.position.x = x
    camera.position.y = y
  }

  override fun scrolled(amount: Int): Boolean {
    camera.zoom = (sign(amount.toFloat()) * ZOOM_SPEED + camera.zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
    return true
  }
}
