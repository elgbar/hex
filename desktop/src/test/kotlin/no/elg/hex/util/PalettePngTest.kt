package no.elg.hex.util

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.GdxNativesLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

/**
 * Tests for [PalettePng].
 *
 * These live in `:desktop` rather than `:core` because decoding the produced PNG requires the native gdx2d image
 * loader, which is what actually reads island previews at runtime. Verifying against it means the tests fail if
 * libGDX ever stops accepting palette PNGs.
 */
class PalettePngTest {

  companion object {

    /** Any opaque colour will do here; the encoder takes it as a parameter so the tests need no libGDX app. */
    private const val BACKGROUND = 0x172D62

    @JvmStatic
    @BeforeAll
    fun loadNatives() {
      GdxNativesLoader.load()
    }

    /** Build RGBA8888 bytes from a `0xRRGGBBAA` per pixel function. */
    private fun image(width: Int, height: Int, pixel: (x: Int, y: Int) -> Int): ByteArray {
      val bytes = ByteArray(width * height * 4)
      for (y in 0 until height) {
        for (x in 0 until width) {
          val offset = (y * width + x) * 4
          val colour = pixel(x, y)
          bytes[offset] = (colour ushr 24).toByte()
          bytes[offset + 1] = (colour ushr 16).toByte()
          bytes[offset + 2] = (colour ushr 8).toByte()
          bytes[offset + 3] = colour.toByte()
        }
      }
      return bytes
    }

    /** The `0xRRGGBB` of a decoded pixel, dropping alpha. */
    private fun Pixmap.rgbAt(x: Int, y: Int): Int = getPixel(x, y) ushr 8 and 0xFFFFFF

    private fun decode(encoded: ByteArray): Pixmap = Pixmap(encoded, 0, encoded.size)

    /** What libGDX itself would have written for the same pixels, used as the size baseline. */
    private fun encodeWithPixmapIo(width: Int, height: Int, rgba: ByteArray): ByteArray {
      val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
      try {
        for (y in 0 until height) {
          for (x in 0 until width) {
            val offset = (y * width + x) * 4
            val colour = (rgba[offset].toInt() and 0xFF shl 24) or
              (rgba[offset + 1].toInt() and 0xFF shl 16) or
              (rgba[offset + 2].toInt() and 0xFF shl 8) or
              (rgba[offset + 3].toInt() and 0xFF)
            pixmap.drawPixel(x, y, colour)
          }
        }
        val out = ByteArrayOutputStream()
        val writer = PixmapIO.PNG()
        try {
          writer.setFlipY(false)
          writer.setCompression(Deflater.BEST_COMPRESSION)
          writer.write(out, pixmap)
        } finally {
          writer.dispose()
        }
        return out.toByteArray()
      } finally {
        pixmap.dispose()
      }
    }

    /** A preview shaped image: a large flat background with a handful of flat shapes and one gradient edge. */
    private fun previewLike(size: Int): ByteArray =
      image(size, size) { x, y ->
        val centre = size / 2
        val distance = Math.hypot((x - centre).toDouble(), (y - centre).toDouble()).toInt()
        when {
          distance > centre - 2 -> 0x00000000
          distance > centre - 12 -> 0xC02040FF.toInt()
          (x / 16 + y / 16) % 2 == 0 -> 0x3070C0FF.toInt()
          else -> 0x2050A0FF.toInt()
        }
      }
  }

  @Test
  fun `produces a png that libgdx can decode at the original size`() {
    val encoded = PalettePng.encode(64, 32, previewLike(64).copyOf(64 * 32 * 4), BACKGROUND)
    val decoded = decode(encoded)
    try {
      assertEquals(64, decoded.width)
      assertEquals(32, decoded.height)
    } finally {
      decoded.dispose()
    }
  }

  @Test
  fun `images within the palette limit round trip exactly`() {
    // 16 * 16 = 256 distinct opaque colours, exactly the palette limit, so nothing may be remapped
    val rgba = image(16, 16) { x, y -> (x * 16 shl 24) or (y * 16 shl 16) or (0x80 shl 8) or 0xFF }
    val decoded = decode(PalettePng.encode(16, 16, rgba, BACKGROUND))
    try {
      for (y in 0 until 16) {
        for (x in 0 until 16) {
          val offset = (y * 16 + x) * 4
          val expected = (rgba[offset].toInt() and 0xFF shl 16) or
            (rgba[offset + 1].toInt() and 0xFF shl 8) or
            (rgba[offset + 2].toInt() and 0xFF)
          assertEquals(expected, decoded.rgbAt(x, y), "pixel ($x, $y) was not preserved exactly")
        }
      }
    } finally {
      decoded.dispose()
    }
  }

  @Test
  fun `fully transparent pixels become the background colour`() {
    val rgba = image(4, 4) { _, _ -> 0x00000000 }
    val decoded = decode(PalettePng.encode(4, 4, rgba, BACKGROUND))
    try {
      assertEquals(BACKGROUND, decoded.rgbAt(0, 0))
      assertEquals(BACKGROUND, decoded.rgbAt(3, 3))
    } finally {
      decoded.dispose()
    }
  }

  @Test
  fun `half transparent pixels are blended towards the background`() {
    val white = 0xFFFFFF80.toInt() // opaque white at 50% alpha
    val decoded = decode(PalettePng.encode(2, 2, image(2, 2) { _, _ -> white }, BACKGROUND))
    try {
      val blended = decoded.rgbAt(0, 0)
      assertNotEquals(0xFFFFFF, blended, "a half transparent pixel must not stay fully white")
      assertNotEquals(BACKGROUND, blended, "a half transparent pixel must not collapse to the background")
      // each channel should sit roughly midway between white and the background
      val red = blended ushr 16 and 0xFF
      val backgroundRed = BACKGROUND ushr 16 and 0xFF
      assertTrue(red > backgroundRed + 50, "expected red to be blended upwards, was $red")
    } finally {
      decoded.dispose()
    }
  }

  @Test
  fun `rare colours snap to their nearest frequent neighbour`() {
    // 256 frequent base colours (16 pixels each) plus a scattering of rare near duplicates offset by 3 on red.
    // Frequency ordering must keep the base colours and remap only the rare ones, moving each by at most that offset.
    val size = 64
    val offset = 3
    fun base(index: Int) = index % PalettePng.MAX_PALETTE_SIZE
    fun isRare(index: Int) = index % 97 == 0

    val rgba = image(size, size) { x, y ->
      val index = y * size + x
      val b = base(index)
      // guard the offset so red never exceeds a byte, which would wrap and leave the colour with no near neighbour
      val red = if (isRare(index) && b + offset <= 0xFF) b + offset else b
      (red shl 24) or ((255 - b) shl 16) or ((b * 7) % 256 shl 8) or 0xFF
    }

    val decoded = decode(PalettePng.encode(size, size, rgba, BACKGROUND))
    try {
      var worst = 0
      for (y in 0 until size) {
        for (x in 0 until size) {
          val index = y * size + x
          val at = index * 4
          val actual = decoded.rgbAt(x, y)
          val expected = ((rgba[at].toInt() and 0xFF) shl 16) or
            ((rgba[at + 1].toInt() and 0xFF) shl 8) or
            (rgba[at + 2].toInt() and 0xFF)
          if (!isRare(index)) {
            assertEquals(expected, actual, "frequent colour at ($x, $y) must survive quantisation untouched")
          }
          for (shift in intArrayOf(16, 8, 0)) {
            worst = maxOf(worst, Math.abs((expected ushr shift and 0xFF) - (actual ushr shift and 0xFF)))
          }
        }
      }
      assertTrue(worst <= offset + 1, "rare colours should snap to a neighbour within $offset, worst delta was $worst")
    } finally {
      decoded.dispose()
    }
  }

  @Test
  fun `is smaller than what PixmapIO would write`() {
    val size = 256
    val rgba = previewLike(size)
    val palette = PalettePng.encode(size, size, rgba, BACKGROUND)
    val rgbaPng = encodeWithPixmapIo(size, size, rgba)
    assertTrue(
      palette.size < rgbaPng.size,
      "palette png (${palette.size} bytes) should be smaller than the RGBA png (${rgbaPng.size} bytes)"
    )
  }

  @Test
  fun `rejects a buffer that does not match the given dimensions`() {
    assertThrows<IllegalArgumentException> { PalettePng.encode(8, 8, ByteArray(8 * 8 * 4 - 1), BACKGROUND) }
  }

  @Test
  fun `rejects an empty image`() {
    assertThrows<IllegalArgumentException> { PalettePng.encode(0, 8, ByteArray(0), BACKGROUND) }
  }
}
