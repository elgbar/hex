package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import ktx.app.KtxInputAdapter
import no.elg.hex.Hex

abstract class AbstractInput(private val useGesture: Boolean = false) :
  KtxInputAdapter,
  GestureDetector.GestureListener {

  private val detector by lazy {
    GestureDetector(
      20f,
      0.4f,
      0.8f,
      Float.MAX_VALUE,
      this
    )
  }

  private val unprojectVector = Vector3()
  private var lastMouseFrame: Long = -1
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

  /**
   * Update the world mouse position. Will only update if the frame has changed since last called
   */
  fun updateMouse() {
    if (lastMouseFrame == Gdx.graphics.frameId) return
    unprojectVector.x = Gdx.input.x.toFloat()
    unprojectVector.y = Gdx.input.y.toFloat()

    Hex.screen.camera.unproject(unprojectVector)
    mouseX = unprojectVector.x
    mouseY = unprojectVector.y
  }

  open fun show() {
    Hex.inputMultiplexer.addProcessor(this)
    if (useGesture) {
      Hex.inputMultiplexer.addProcessor(detector)
    }
  }

  override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean = false

  override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean = false

  override fun longPress(x: Float, y: Float): Boolean = false

  override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean = false

  override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean = false

  override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean = false

  override fun zoom(initialDistance: Float, distance: Float): Boolean = false

  override fun pinch(initialPointer1: Vector2, initialPointer2: Vector2, pointer1: Vector2, pointer2: Vector2): Boolean = false

  override fun pinchStop() = Unit
}