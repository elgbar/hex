package no.elg.hex.util

import com.badlogic.gdx.Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.utils.ScreenUtils
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

/** Encode the contents of this frame buffer as a palette PNG, see [PalettePng.encode]. */
fun FrameBuffer.toBytes(): ByteArray {
  val encoded: ByteArray
  this.safeUse {
    val pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, false)
    encoded = PalettePng.encode(width, height, pixels)
  }
  return encoded
}

fun FrameBuffer.takeScreenshot(fileHandle: FileHandle) = fileHandle.writeBytes(toBytes(), false)

fun textureFromBytes(encoded: ByteArray): Texture = Texture(Pixmap(encoded, 0, encoded.size))

fun Camera.resetHdpi() = HdpiUtils.glViewport(0, 0, viewportWidth.toInt(), viewportHeight.toInt())

private val futureRequestRenderTimer = Timer()
private var wantedRenderTime = Long.MAX_VALUE
private var currentTask: TimerTask? = null

fun Graphics.requestRenderingIn(seconds: Float) {
  require(seconds >= 0) { "Seconds must be positive" }
  synchronized(futureRequestRenderTimer) {
    val delayMs = (seconds * 1000.0).toLong()
    val nextRenderTime = System.currentTimeMillis() + delayMs
    if (nextRenderTime < wantedRenderTime) {
      wantedRenderTime = nextRenderTime
      currentTask?.cancel()
      currentTask = futureRequestRenderTimer.schedule(delayMs) {
        synchronized(futureRequestRenderTimer) {
          if (wantedRenderTime == nextRenderTime) {
            wantedRenderTime = Long.MAX_VALUE
          }
          requestRendering()
        }
      }
    }
  }
}