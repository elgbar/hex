package src.no.elg.hex

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import org.hexworks.mixite.core.api.Hexagon
import src.no.elg.hex.hexagon.HexUtil
import src.no.elg.hex.hexagon.HexagonData
import java.awt.Toolkit

/**
 * @author Elg
 */
object InputHandler : FrameUpdatable {


  val scale: Int = if (Toolkit.getDefaultToolkit().screenSize.width > 2560) 2 else 1

  private val mouse = Vector2()
  private val mouseVec = Vector3()

  val mouseX get() = mouseVec.x
  val mouseY get() = mouseVec.y

  val cursorHex: Hexagon<HexagonData>? get() = HexUtil.getHexagon(mouseX.toDouble(), mouseY.toDouble())

  override fun frameUpdate() {
    mouseVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
    mouse.set(mouseX, mouseY)
  }
}
