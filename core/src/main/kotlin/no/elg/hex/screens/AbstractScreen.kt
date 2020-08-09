package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera

/** @author Elg */
abstract class AbstractScreen : ScreenAdapter() {

  val camera: OrthographicCamera by lazy {
    OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).also {
      it.setToOrtho(true)
    }
  }

  abstract override fun render(delta: Float)

  override fun resize(width: Int, height: Int) {
    camera.setToOrtho(true, width.toFloat(), height.toFloat())
  }
}
