package src.no.elg.hex.rendrer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable
import src.no.elg.hex.InputHandler.scale
import src.no.elg.hex.Resizable
import java.io.File

object ScreenRenderer : Disposable, Resizable {

  val spacing: Int
  val font: BitmapFont
  val batch: SpriteBatch

  fun drawTop(text: String?, line: Int) {
    font.draw(batch, text, spacing.toFloat(), Gdx.graphics.height - spacing * line * 2f)
  }

  fun drawBottom(text: String?, line: Int) {
    font.draw(batch, text, spacing.toFloat(), spacing * (line + 1).toFloat())
  }

  fun begin() {
    batch.begin()
  }

  fun end() {
    batch.end()
  }

  fun resetFontColor() {
    font.color = Color.WHITE
  }

  override fun dispose() {
    batch.dispose()
    font.dispose()
  }

  override fun resize(width: Int, height: Int) {
    batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
  }

  val FONTS_FOLDER = "fonts" + File.separatorChar
  const val FONT_SIZE = 20

  init {
    val generator = FreeTypeFontGenerator(Gdx.files.internal(FONTS_FOLDER + "UbuntuMono-R.ttf"))
    val parameter = FreeTypeFontParameter()
    parameter.size = FONT_SIZE * scale
    parameter.minFilter = Linear

    font = generator.generateFont(parameter)
    spacing = FONT_SIZE * scale / 2

    batch = SpriteBatch()
    batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
  }
}
