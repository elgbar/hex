package no.elg.hex.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.utils.Base64Coder
import com.badlogic.gdx.utils.Base64Coder.urlsafeMap
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.TimeUtils
import ktx.graphics.use
import java.util.zip.Deflater

fun FrameBuffer.takeScreenshot(fileHandle: FileHandle) {
  this.use {
    val bufferWidth = this.width
    val bufferHeight = this.height
    val pixels = ScreenUtils.getFrameBufferPixels(0, 0, bufferWidth, bufferHeight, true)

    val screenshotImage = Pixmap(bufferWidth, bufferHeight, Pixmap.Format.RGBA8888)
    BufferUtils.copy(pixels, 0, screenshotImage.pixels, pixels.size)
    PixmapIO.writePNG(fileHandle, screenshotImage, Deflater.BEST_COMPRESSION, true)
    screenshotImage.dispose()
  }
}

fun FrameBuffer.saveScreenshotAsString(): String {
  val tmpFile = Gdx.files.local("tmp/${TimeUtils.nanoTime()}")
  takeScreenshot(tmpFile)
  return String(Base64Coder.encode(tmpFile.readBytes(), urlsafeMap)).also {
    tmpFile.delete()
  }
}

fun decodeStringToTexture(encoded: String): Texture {
  val decoded = Base64Coder.decode(encoded, urlsafeMap)
  return Texture(Pixmap(decoded, 0, decoded.size))
}

fun Camera.resetHdpi() = HdpiUtils.glViewport(0, 0, viewportWidth.toInt(), viewportHeight.toInt())
