package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.draw

/**
 * @author Elg
 */
object MessagesRenderer : FrameUpdatable {

  const val DURATION_SECONDS = 3f
  const val FADE_START = 0.5f

  private val messages = ArrayList<Pair<ScreenText, Float>>()

  fun publishMessage(message: String) {
    publishMessage(ScreenText(message, color = Color.LIGHT_GRAY))
  }

  fun publishMessage(message: ScreenText) {
    messages.add(0, message to DURATION_SECONDS)
  }

  override fun frameUpdate() {
    val newMessages = ArrayList<Pair<ScreenText, Float>>()

    ScreenRenderer.begin()
    for ((index, pair) in messages.withIndex()) {
      val (message, timeLeft) = pair


      if (timeLeft < FADE_START) {
        val alpha = timeLeft / FADE_START
        message.copy(color = message.color.cpy().also { it.a = alpha })
      } else {
        message
      }.draw(index + 1, ScreenDrawPosition.BOTTOM_RIGHT)


      val newTime = timeLeft - Gdx.graphics.rawDeltaTime
      if (newTime > 0f) {
        newMessages.add(message to newTime)
      }
    }
    ScreenRenderer.end()

    messages.clear()
    messages.addAll(newMessages)
  }
}
