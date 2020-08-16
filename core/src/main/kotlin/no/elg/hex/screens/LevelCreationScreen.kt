package no.elg.hex.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import javax.sound.sampled.Line
import no.elg.hex.Assets
import no.elg.hex.Hex
import no.elg.hex.input.LevelCreationInputProcessor

/** @author Elg */
object LevelCreationScreen : AbstractScreen() {

  override fun render(delta: Float) {
    lineRenderer.begin(ShapeType.Line)
    lineRenderer.color = Color.LIGHT_GRAY
    lineRenderer.line(
        Gdx.graphics.width / 2f, 0f, Gdx.graphics.width / 2f, Gdx.graphics.height.toFloat())
    lineRenderer.line(
        0f, Gdx.graphics.height / 2f, Gdx.graphics.width.toFloat(), Gdx.graphics.height / 2f)
    lineRenderer.end()

    batch.begin()

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

    Hex.assets.regularFont.draw(
        batch,
        text,
        (Gdx.graphics.width - longestLine / 2 * Assets.fontSize) / 2f,
        (Gdx.graphics.height - lines * Hex.assets.regularFont.capHeight) / 2f)

    batch.end()
  }

  override fun show() {
    Hex.inputMultiplexer.addProcessor(LevelCreationInputProcessor)
  }

  override fun hide() {
    Hex.inputMultiplexer.removeProcessor(LevelCreationInputProcessor)
  }
}
