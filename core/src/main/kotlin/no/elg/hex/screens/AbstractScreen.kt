package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import no.elg.hex.Hex
import no.elg.hex.util.isLazyInitialized

/** @author Elg */
abstract class AbstractScreen : ScreenAdapter() {

  protected val batch: SpriteBatch by lazy { SpriteBatch() }
  protected val lineRenderer: ShapeRenderer by lazy { ShapeRenderer() }

  var isDisposed = false
    private set

  val camera: OrthographicCamera by lazy {
    OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
  }

  abstract override fun render(delta: Float)

  fun updateCamera() {
    camera.update()
    batch.projectionMatrix = camera.combined
    lineRenderer.projectionMatrix = camera.combined
  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(true, width.toFloat(), height.toFloat())
    updateCamera()

    if (::batch.isLazyInitialized) {
      batch.projectionMatrix = camera.combined
    }
    if (::lineRenderer.isLazyInitialized) {
      lineRenderer.projectionMatrix = camera.combined
    }
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
    if (::batch.isLazyInitialized) batch.dispose()
    if (::lineRenderer.isLazyInitialized) lineRenderer.dispose()
    isDisposed = true
  }
}
