package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.Align
import no.elg.hex.Assets
import no.elg.hex.Hex

/**
 * @author Elg
 */
object SplashScreen : AbstractScreen() {

  private val font: BitmapFont by lazy { Hex.assets.get<BitmapFont>(Assets.REGULAR_FONT) }
  private val startTime: Long by lazy { System.currentTimeMillis() }
  private val layout by lazy { GlyphLayout(font, "") }

  override fun render(delta: Float) {

    if (Hex.assets.finishMainConst && Hex.assets.update()) {
      Gdx.app.log("Splash", "All assets finished loading")
      Hex.screen = LevelSelectScreen
    } else {
      camera.update()
      batch.begin()

      val txt = "LOADING %2.0f%% %n%n${System.currentTimeMillis() - startTime} ms".format(Hex.assets.progress * 100)
      layout.setText(font, txt, Color.WHITE, Gdx.graphics.width.toFloat(), Align.center, true)
      font.draw(batch, layout, 0f, Gdx.graphics.height.toFloat() / 2)
      batch.end()
    }
  }
}

