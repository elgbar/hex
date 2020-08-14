package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import no.elg.hex.util.isLazyInitialized

/** @author Elg */
abstract class AbstractScreen : ScreenAdapter() {

  protected val batch: SpriteBatch by lazy { SpriteBatch() }
  protected val lineRenderer: ShapeRenderer by lazy { ShapeRenderer() }

  val camera: OrthographicCamera by lazy {
    OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).also {
      it.setToOrtho(true)
    }
  }

  abstract override fun render(delta: Float)

  fun updateCamera() {
    camera.update()
    batch.projectionMatrix = camera.combined
    lineRenderer.projectionMatrix = camera.combined
  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(true, width.toFloat(), height.toFloat())

    if (::batch.isLazyInitialized) {
      batch.projectionMatrix = camera.combined
    }
    if (::lineRenderer.isLazyInitialized) {
      lineRenderer.projectionMatrix = camera.combined
    }
  }
}
