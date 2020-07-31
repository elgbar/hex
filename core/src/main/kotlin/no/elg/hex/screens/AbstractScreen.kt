package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * @author Elg
 */
abstract class AbstractScreen : ScreenAdapter() {


  val camera: OrthographicCamera by lazy {
    OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).also { it.setToOrtho(true) }
  }
  protected val batch: SpriteBatch by lazy { SpriteBatch() }

  abstract override fun render(delta: Float)

  /**
   * Called on the main thread when this screen is set as the current screen
   */
  open fun onLoad() {}

  open fun onUnload() {}

  override fun dispose() {
    batch.dispose();
  }

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(true, width.toFloat(), height.toFloat())
  }
}
