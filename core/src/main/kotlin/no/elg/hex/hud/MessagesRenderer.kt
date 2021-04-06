package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.LIGHT_GRAY
import com.badlogic.gdx.graphics.Color.RED
import com.badlogic.gdx.graphics.Color.YELLOW
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.draw
import java.util.concurrent.CopyOnWriteArrayList

/** @author Elg */
object MessagesRenderer : FrameUpdatable {

  const val DEFAULT_DURATION_SECONDS = 10f
  private const val FADE_START = 0.5f

  private val messages = CopyOnWriteArrayList<Pair<ScreenText, Float>>()

  fun publishMessage(message: String, durationSeconds: Float = DEFAULT_DURATION_SECONDS, color: Color = LIGHT_GRAY) {
    val sst = staticTextPool.obtain()
    sst.text = message
    sst.color = color
    publishMessage(sst, durationSeconds)
  }

  fun publishWarning(message: String, durationSeconds: Float = DEFAULT_DURATION_SECONDS) {
    publishMessage(message, durationSeconds, YELLOW)
  }

  fun publishError(message: String, durationSeconds: Float = DEFAULT_DURATION_SECONDS) {
    val sst = staticTextPool.obtain()
    sst.text = message
    sst.color = RED
    Gdx.app.error("ERR MSG", sst.wholeText)
    messages.add(0, sst to durationSeconds)
  }

  fun publishMessage(message: ScreenText, durationSeconds: Float = DEFAULT_DURATION_SECONDS) {
    Gdx.app.log("MESSAGE", message.wholeText)
    messages.add(0, message to durationSeconds)
  }

  override fun frameUpdate() {
    if (messages.isEmpty()) return
    val newMessages = ArrayList<Pair<ScreenText, Float>>()

    ScreenRenderer.begin()
    for ((index, pair) in messages.withIndex()) {
      val (message, timeLeft) = pair

      if (timeLeft < FADE_START) {
        val alpha = timeLeft / FADE_START
        message.setAlpha(alpha)
      } else {
        message
      }.draw(index + 1, ScreenDrawPosition.BOTTOM_RIGHT)

      val newTime = timeLeft - Gdx.graphics.deltaTime
      if (newTime > 0f) {
        newMessages.add(message to newTime)
      } else if (message is StaticScreenText) {
        staticTextPool.free(message)
      }
    }
    ScreenRenderer.end()

    messages.clear()
    messages.addAll(newMessages)
  }

  private fun ScreenText.setAlpha(alpha: Float): ScreenText {
    color = this.color.cpy().also { it.a = alpha }
    return this
  }
}
