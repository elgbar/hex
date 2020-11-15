package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.draw

/** @author Elg */
object MessagesRenderer : FrameUpdatable {

  private const val DURATION_SECONDS = 10f
  private const val FADE_START = 0.5f

  private val messages = ArrayList<Pair<ScreenText, Float>>()

  fun publishMessage(message: String) {
    publishMessage(ScreenText(message, color = Color.LIGHT_GRAY))
  }

  fun publishWarning(message: String) {
    publishMessage(ScreenText(message, color = Color.YELLOW))
  }

  fun publishError(message: String) {
    publishMessage(ScreenText(message, color = Color.RED))
  }

  fun publishMessage(message: ScreenText) {
    Gdx.app.log("MESSAGE", message.text)
    messages.add(0, message to DURATION_SECONDS)
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
