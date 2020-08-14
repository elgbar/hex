package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.Assets.Companion.FONT_SIZE
import no.elg.hex.Hex
import no.elg.hex.input.LevelCreationInputProcessor

/** @author Elg */
object LevelCreationScreen : AbstractScreen() {

  override fun render(delta: Float) {
    //    lineRenderer.begin(Line)
    //    lineRenderer.color = Color.LIGHT_GRAY
    //    lineRenderer.line(Gdx.graphics.width / 2f, 0f, Gdx.graphics.width / 2f,
    // Gdx.graphics.height.toFloat())
    //    lineRenderer.line(0f, Gdx.graphics.height / 2f, Gdx.graphics.width.toFloat(),
    // Gdx.graphics.height / 2f)
    //    lineRenderer.end()

    batch.begin()
    Hex.assets.regularFont.color = Color.WHITE

    val text =
        """
      |Width: ${LevelCreationInputProcessor.width}
      |Height: ${LevelCreationInputProcessor.height}
      |Layout: ${LevelCreationInputProcessor.layout}
      |
      |Click to create island
    """.trimMargin()

    val longestLine = (text.lineSequence().maxBy { it.length }?.length ?: text.length)
    val lines = text.lineSequence().count()

    //    layout.setText(Hex.assets.regularFont, text, Color.WHITE, Gdx.graphics.width.toFloat(),
    // Align.center, true)
    Hex.assets.regularFont.draw(
        batch,
        text,
        Gdx.graphics.width / 2f - longestLine / 4f * FONT_SIZE,
        Gdx.graphics.height / 2f + (lines / 2f) * Hex.assets.regularFont.capHeight)

    batch.end()
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(LevelCreationInputProcessor)
  }

  override fun hide() {
    Hex.inputMultiplexer.removeProcessor(LevelCreationInputProcessor)
  }
}
