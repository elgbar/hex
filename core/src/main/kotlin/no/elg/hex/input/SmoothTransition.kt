package no.elg.hex.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Interpolation
import no.elg.hex.screens.PreviewIslandScreen

data class SmoothTransition(
  private val screen: PreviewIslandScreen,
  val zoomTarget: Float,
  val xTarget: Float?,
  val yTarget: Float?,
  val transitionDurationSeconds: Float = .25f
) {

  private var timeToTarget = transitionDurationSeconds

  private val cameraZoomOrigin: Float = camera.zoom
  private val xPositionOrigin: Float = camera.position.x
  private val yPositionOrigin: Float = camera.position.y

  private val camera get() = screen.camera

  /**
   * @return If the zoom is complete
   */
  fun zoom(delta: Float): Boolean {
    if (timeToTarget >= 0) {
      timeToTarget -= delta

      val progress: Float = if (timeToTarget < 0) 1f else 1f - timeToTarget / transitionDurationSeconds
      camera.zoom = Interpolation.smooth.apply(cameraZoomOrigin, zoomTarget, progress)
      if(xTarget != null && yTarget != null) {
        camera.position.x = Interpolation.smooth.apply(xPositionOrigin, xTarget, progress)
        camera.position.y = Interpolation.smooth.apply(yPositionOrigin, yTarget, progress)
      }
      screen.enforceCameraBounds()
      camera.update()

      Gdx.graphics.requestRendering()
      return false
    }
    return true
  }
}