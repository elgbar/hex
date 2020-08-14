package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import no.elg.hex.Hex

/** @author Elg */
object SplashScreen : AbstractScreen() {

  private var startTime: Long = System.currentTimeMillis()

  private val layout by lazy { GlyphLayout() }

  override fun show() {
    startTime = System.currentTimeMillis()
  }

  override fun render(delta: Float) {

    if (Hex.assets.finishMainConst && Hex.assets.update()) {
      Gdx.app.log("SPLASH", "All assets finished loading")
      Hex.screen = LevelSelectScreen
    } else {
      camera.update()
      batch.begin()

      val txt =
          "Loading ${Hex.assets.loadingInfo}%n%n%2.0f%%%n%n${System.currentTimeMillis() - startTime} ms".format(
              Hex.assets.progress * 100)
      layout.setText(
          Hex.assets.regularFont,
          txt,
          Color.WHITE,
          Gdx.graphics.width.toFloat(),
          Align.center,
          true)
      Hex.assets.regularFont.draw(batch, layout, 0f, Gdx.graphics.height.toFloat() / 2)
      batch.end()
    }
  }
}
