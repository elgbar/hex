package no.elg.hex.util

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
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

fun Camera.resetHdpi() = HdpiUtils.glViewport(0, 0, viewportWidth.toInt(), viewportHeight.toInt())
