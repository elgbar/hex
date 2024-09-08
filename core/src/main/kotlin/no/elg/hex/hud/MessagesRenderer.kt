package no.elg.hex.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.LIGHT_GRAY
import com.badlogic.gdx.graphics.Color.RED
import com.badlogic.gdx.graphics.Color.YELLOW
import no.elg.hex.api.FrameUpdatable
import no.elg.hex.hud.ScreenRenderer.draw
import no.elg.hex.util.requestRenderingIn
import java.util.concurrent.CopyOnWriteArrayList

/** @author Elg */
object MessagesRenderer : FrameUpdatable {

  private const val DEFAULT_DURATION_SECONDS = 10f
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

  fun publishError(message: String, durationSeconds: Float = DEFAULT_DURATION_SECONDS, exception: Throwable? = null) {
    val sst = staticTextPool.obtain()
    sst.text = message
    sst.color = RED
    if (exception == null) {
      Gdx.app.error("ERR MSG", sst.wholeText)
    } else {
      Gdx.app.error("ERR MSG", sst.wholeText, exception)
    }
    messages.add(0, sst to durationSeconds)
  }

  fun publishMessage(message: ScreenText, durationSeconds: Float = DEFAULT_DURATION_SECONDS, exception: Exception? = null) {
    if (exception == null) {
      Gdx.app.log("MESSAGE", message.wholeText)
    } else {
      Gdx.app.log("MESSAGE", message.wholeText, exception)
    }
    messages.add(0, message to durationSeconds)
  }

  override fun frameUpdate() {
    if (messages.isEmpty()) return
    val newMessages = ArrayList<Pair<ScreenText, Float>>()

    ScreenRenderer.use {
      for ((index, pair) in messages.withIndex()) {
        val (message, lastTimeLeft) = pair

        val updatedTime = lastTimeLeft - Gdx.graphics.deltaTime
        if (updatedTime > 0f) {
          if (updatedTime < FADE_START) {
            Gdx.graphics.requestRendering()
            val alpha = updatedTime / FADE_START
            message.setAlpha(alpha)
          } else {
            message
          }.draw(index + 1, ScreenDrawPosition.BOTTOM_RIGHT)
          newMessages.add(message to updatedTime)
        } else if (message is StaticScreenText) {
          staticTextPool.free(message)
        }
      }
    }
    messages.clear()
    messages.addAll(newMessages)

    newMessages.map { it.second - FADE_START }.filter { it > 0 }.minOrNull()?.also {
      Gdx.graphics.requestRenderingIn(it)
    }
  }

  private fun ScreenText.setAlpha(alpha: Float): ScreenText {
    color = this.color.cpy().also { it.a = alpha }
    return this
  }
}