package src.no.elg.hex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import no.elg.hex.Hex
import org.hexworks.mixite.core.api.Hexagon
import src.no.elg.hex.hexagon.HexUtil
import src.no.elg.hex.hexagon.HexagonData
import java.awt.Toolkit
import kotlin.math.abs

/**
 * @author Elg
 */
object InputHandler : InputAdapter(), FrameUpdatable {

  private const val ZOOM_ENABLE = false
  private var totalZoom = 1f

  private const val MIN_MOVE_AMOUNT = 0

  private const val MIN_ZOOM = 0.15f
  private const val MAX_ZOOM = 3.0f

  /** Higher means lower zoom speed  */
  private const val ZOOM_SPEED = 5

  var cameraOffsetX = 0f
    private set
  var cameraOffsetY = 0f
    private set

  val scale: Int = if (Toolkit.getDefaultToolkit().screenSize.width > 2560) 2 else 1

  private val mouse = Vector2()
  private val mouseVec = Vector3()

  val mouseX get() = mouseVec.x
  val mouseY get() = mouseVec.y

  val cursorHex: Hexagon<HexagonData>? get() = HexUtil.getHexagon(mouseX.toDouble() - cameraOffsetX, mouseY.toDouble() - cameraOffsetY)

  override fun frameUpdate() {
    mouseVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
    mouse.set(mouseX, mouseY)
  }

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    val x: Float = Gdx.input.deltaX * totalZoom
    val y: Float = Gdx.input.deltaY * totalZoom

    //Make the little movements when clicking fast less noticeable
    if (abs(x) < MIN_MOVE_AMOUNT * totalZoom && abs(y) < MIN_MOVE_AMOUNT * totalZoom) {
      return false
    }

    cameraOffsetX += x
    cameraOffsetY += y

    return true
  }

  fun resetCamera() {
    val data = Hex.world.grid.gridData
    cameraOffsetX = -((data.gridWidth * data.hexagonWidth + data.gridWidth / 2f - Gdx.graphics.width) / 2f).toFloat()
    cameraOffsetY = ((data.gridHeight * data.hexagonHeight + data.gridHeight / 2f - Gdx.graphics.height) / 2f).toFloat()
    Hex.camera.translate(cameraOffsetX, cameraOffsetY)
  }

  private fun moveCamera(dx: Float, dy: Float) {
    cameraOffsetX += dx
    cameraOffsetY += dy
    Hex.camera.translate(cameraOffsetX, cameraOffsetY)
  }
}
