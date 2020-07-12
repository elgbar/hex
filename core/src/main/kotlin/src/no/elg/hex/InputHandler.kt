package src.no.elg.hex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import no.elg.hex.Hex
import org.hexworks.mixite.core.api.Hexagon
import src.no.elg.hex.hexagon.HexUtil
import src.no.elg.hex.hexagon.HexagonData
import java.awt.Toolkit
import kotlin.Float.Companion
import kotlin.math.abs
import kotlin.math.sign

/**
 * @author Elg
 */
object InputHandler : InputAdapter() {

  private const val ZOOM_ENABLE = false

  private const val MIN_MOVE_AMOUNT = 0

  const val MIN_ZOOM = 0.15f
  const val MAX_ZOOM = 3.0f

  private const val ZOOM_SPEED = 0.1f

  var cameraOffsetX = 0f
    private set
  var cameraOffsetY = 0f
    private set

  val scale: Int = if (Toolkit.getDefaultToolkit().screenSize.width > 2560) 2 else 1

  val mouseX get() = Gdx.input.x
  val mouseY get() = Gdx.input.y

  val cursorHex: Hexagon<HexagonData>? get() = HexUtil.getHexagon(mouseX.toDouble() - cameraOffsetX, mouseY.toDouble() - cameraOffsetY)

  override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
    val zoom = Hex.camera.zoom
    val x: Float = Gdx.input.deltaX * zoom
    val y: Float = Gdx.input.deltaY * zoom

    //Make the little movements when clicking fast less noticeable
    if (abs(x) < MIN_MOVE_AMOUNT * zoom && abs(y) < MIN_MOVE_AMOUNT * zoom) {
      return false
    }

    cameraOffsetX += x
    cameraOffsetY += y

    return true
  }

  fun resetCamera() {
    val data = Hex.world.grid.gridData
    cameraOffsetX = -((data.gridWidth * data.hexagonWidth + data.gridWidth / 2f - Gdx.graphics.width) / 2f).toFloat()
    cameraOffsetY = -((data.gridHeight * data.hexagonHeight + data.gridHeight / 2f - Gdx.graphics.height) / 2f).toFloat()
  }

  override fun scrolled(amount: Int): Boolean {
    val dir = sign(amount.toFloat())
    Hex.camera.zoom = (dir * ZOOM_SPEED + Hex.camera.zoom).coerceIn(Float.MIN_VALUE, Companion.MAX_VALUE)
    return true
  }
}
