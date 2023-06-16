package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.TimeUtils
import ktx.graphics.use
import java.util.Timer
import java.util.TimerTask
import java.util.zip.Deflater
import kotlin.concurrent.schedule

fun FrameBuffer.takeScreenshot(fileHandle: FileHandle) {
  this.use {
    val bufferWidth = this.width
    val bufferHeight = this.height
    val pixels = ScreenUtils.getFrameBufferPixels(0, 0, bufferWidth, bufferHeight, false)

    val screenshotImage = Pixmap(bufferWidth, bufferHeight, Pixmap.Format.RGBA8888)
    BufferUtils.copy(pixels, 0, screenshotImage.pixels, pixels.size)
    PixmapIO.writePNG(fileHandle, screenshotImage, Deflater.BEST_COMPRESSION, false)
    screenshotImage.dispose()
  }
}

fun FrameBuffer.toBytes(): ByteArray {
  val tmpFile = Gdx.files.local("tmp/${TimeUtils.nanoTime()}")
  takeScreenshot(tmpFile)
  return tmpFile.file().readBytes().also {
    tmpFile.delete()
  }
}

fun textureFromBytes(encoded: ByteArray): Texture {
  return Texture(Pixmap(encoded, 0, encoded.size))
}

fun Camera.resetHdpi() = HdpiUtils.glViewport(0, 0, viewportWidth.toInt(), viewportHeight.toInt())

private val futureRequestRenderTimer = Timer()
private var wantedRenderTime = Long.MAX_VALUE
private var currentTask: TimerTask? = null

fun Graphics.requestRenderingIn(seconds: Float) {
  synchronized(futureRequestRenderTimer) {
    val delayMs = (seconds * 1000.0).toLong()
    val nextRenderTime = System.currentTimeMillis() + delayMs
    if (nextRenderTime < wantedRenderTime) {
      wantedRenderTime = nextRenderTime
      currentTask?.cancel()
      currentTask = futureRequestRenderTimer.schedule(delayMs) {
        synchronized(futureRequestRenderTimer) {
//          Gdx.app.trace("requestRenderingIn") { "Requesting rendering, (accuracy is ${System.currentTimeMillis() - nextRenderTime} ms)" }
          if (wantedRenderTime == nextRenderTime) {
            wantedRenderTime = Long.MAX_VALUE
          }
          requestRendering()
        }
      }
    }
  }
}