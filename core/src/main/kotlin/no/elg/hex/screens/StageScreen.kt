package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Scaling.fit
import com.badlogic.gdx.utils.viewport.ScalingViewport
import no.elg.hex.Hex

/** @author Elg */
abstract class StageScreen : AbstractScreen() {
  val stage =
      Stage(
          ScalingViewport(fit, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), camera))

  override fun render(delta: Float) {
    stage.act()
    stage.draw()
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(stage)
  }

  override fun hide() {
    Hex.inputMultiplexer.removeProcessor(stage)
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    camera.setToOrtho(false)
    stage.viewport.setWorldSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    stage.viewport.setScreenSize(Gdx.graphics.width, Gdx.graphics.height)
  }
}
