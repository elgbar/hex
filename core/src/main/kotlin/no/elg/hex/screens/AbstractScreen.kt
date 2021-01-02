package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import no.elg.hex.Hex

/** @author Elg */
abstract class AbstractScreen(val yDown: Boolean = true) : ScreenAdapter() {

  val batch: SpriteBatch by lazy { SpriteBatch() }
  protected val lineRenderer: ShapeRenderer by lazy { ShapeRenderer() }

  var isDisposed = false
    private set

  val camera: OrthographicCamera by lazy {
    OrthographicCamera().apply { setToOrtho(yDown) }
  }

  abstract override fun render(delta: Float)

  fun updateCamera() {
    camera.update()
    batch.projectionMatrix = camera.combined
    lineRenderer.projectionMatrix = camera.combined
  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(yDown, width.toFloat(), height.toFloat())
    updateCamera()
  }

  fun renderBackground() {
    batch.begin()

    batch.disableBlending()

    val bgWidth = Hex.assets.background.packedWidth.toFloat()
    val bgHeight = Hex.assets.background.packedHeight.toFloat()

    var offsetY = 0
    do {
      var offsetX = 0
      do {
        batch.draw(Hex.assets.background, offsetX * bgWidth, offsetY * bgHeight)
        offsetX++
      } while (offsetX < Gdx.graphics.width.toFloat() * camera.zoom)
      offsetY++
    } while (offsetY < Gdx.graphics.height.toFloat() * camera.zoom)

    batch.enableBlending()
    batch.end()
  }

  override fun hide() {
    try {
      dispose()
    } catch (e: Exception) {
    }
  }

  override fun dispose() {
    batch.dispose()
    lineRenderer.dispose()
    isDisposed = true
  }
}
