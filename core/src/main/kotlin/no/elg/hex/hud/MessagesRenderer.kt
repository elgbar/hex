package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.draw
import java.util.concurrent.CopyOnWriteArrayList

/** @author Elg */
object MessagesRenderer : FrameUpdatable {

  const val DEFAULT_DURATION_SECONDS = 10f
  private const val FADE_START = 0.5f

  private val messages = CopyOnWriteArrayList<Pair<ScreenText, Float>>()

  fun publishMessage(message: String, durationSeconds: Float = DEFAULT_DURATION_SECONDS) {
    publishMessage(ScreenText(message, color = Color.LIGHT_GRAY), durationSeconds)
  }

  fun publishWarning(message: String, durationSeconds: Float = DEFAULT_DURATION_SECONDS) {
    publishMessage(ScreenText(message, color = Color.YELLOW), durationSeconds)
  }

  fun publishError(message: String, durationSeconds: Float = DEFAULT_DURATION_SECONDS) {
    publishMessage(ScreenText(message, color = Color.RED), durationSeconds)
  }

  fun publishMessage(message: ScreenText, durationSeconds: Float = DEFAULT_DURATION_SECONDS) {
    Gdx.app.log("MESSAGE", message.wholeText)
    messages.add(0, message to durationSeconds)
  }

  override fun frameUpdate() {
    val newMessages = ArrayList<Pair<ScreenText, Float>>()

    ScreenRenderer.begin()
    for ((index, pair) in messages.withIndex()) {
      val (message, timeLeft) = pair

      if (timeLeft < FADE_START) {
        val alpha = timeLeft / FADE_START
        message.copyAndSetAlpha(alpha)
      } else {
        message
      }
        .draw(index + 1, ScreenDrawPosition.BOTTOM_RIGHT)

      val newTime = timeLeft - Gdx.graphics.rawDeltaTime
      if (newTime > 0f) {
        newMessages.add(message to newTime)
      }
    }
    ScreenRenderer.end()

    messages.clear()
    messages.addAll(newMessages)
  }

  private fun ScreenText.copyAndSetAlpha(alpha: Float): ScreenText {
    return copy(color = this.color.cpy().also { it.a = alpha }, next = next?.copyAndSetAlpha(alpha))
  }
}
