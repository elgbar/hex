package no.elg.hex.util

import com.badlogic.gdx.utils.IntIntMap
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Encodes RGBA8888 pixels as an 8 bit palette PNG (colour type 3).
 *
 * The alpha byte of every pixel is ignored. Previews come from an RGB565 frame buffer, which has no alpha channel at
 * all, so `glReadPixels` always reports 255 and the background is baked in when the buffer is cleared. Feeding this
 * translucent pixels would silently drop the transparency rather than blend it.
 *
 * Island previews are rendered into an RGB565 frame buffer, so they hold only a few thousand distinct colours, nearly
 * all of them antialiasing intermediates along hexagon edges and font glyphs. Indexing into a palette stores one byte
 * per pixel instead of four, and flat regions collapse into runs of identical indices, which deflate handles well.
 * Over all 701 bundled previews that is a 45% reduction while moving 0.13% of pixels.
 *
 * ## The file this writes
 *
 * An 8 byte signature followed by four chunks. Every chunk is `[length: 4][type: 4 ascii][data][crc32: 4]`, where the
 * length covers the data only and the CRC covers type + data.
 *
 * - `IHDR` 13 bytes of width, height, bit depth, colour type, then compression/filter/interlace which each have
 *   exactly one legal value.
 * - `PLTE` three bytes per palette entry, red green blue.
 * - `IDAT` every scanline as one filter byte followed by [width] palette indices, all deflated together.
 * - `IEND` empty.
 *
 * Interlacing, 16 bit depths, `tRNS` and the colour space chunks are all part of PNG but deliberately not emitted:
 * this only ever produces one shape of file.
 */
object PalettePng {

  /** A PNG palette holds at most this many entries at a bit depth of 8. */
  const val MAX_PALETTE_SIZE: Int = 256

  private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte(), 0x1A, '\n'.code.toByte())

  private const val BYTES_PER_RGBA_PIXEL = 4
  private const val BIT_DEPTH = 8
  private const val COLOUR_TYPE_INDEXED = 3

  private const val FILTER_NONE = 0

  private const val DEFLATE_BUFFER_SIZE = 1 shl 16

  /** Sentinel for [IntIntMap.get], which needs a default rather than returning null. */
  private const val NOT_IN_PALETTE = -1

  /** Previews hold a few thousand distinct colours, so start the histogram large enough to avoid rehashing. */
  private const val INITIAL_COLOUR_CAPACITY = 4096

  /** Encode [rgba], which must hold [width] * [height] pixels in RGBA8888 order. Alpha bytes are ignored. */
  fun encode(width: Int, height: Int, rgba: ByteArray): ByteArray {
    require(width > 0 && height > 0) { "Image must not be empty, got ${width}x$height" }
    val expected = width * height * BYTES_PER_RGBA_PIXEL
    require(rgba.size == expected) { "Expected $expected bytes of RGBA8888 data for a ${width}x$height image, got ${rgba.size}" }

    val opaque = toRgbColours(rgba)
    val palette = buildPalette(opaque)
    val indices = mapToPaletteIndices(opaque, palette)

    return buildPng(width, height, indices, palette)
  }

  /** Pack each pixel into a `0xRRGGBB` value, discarding the alpha byte. */
  private fun toRgbColours(rgba: ByteArray): IntArray {
    val out = IntArray(rgba.size / BYTES_PER_RGBA_PIXEL)
    for (pixel in out.indices) {
      val offset = pixel * BYTES_PER_RGBA_PIXEL
      val red = rgba[offset].toInt() and 0xFF
      val green = rgba[offset + 1].toInt() and 0xFF
      val blue = rgba[offset + 2].toInt() and 0xFF
      out[pixel] = red shl 16 or (green shl 8) or blue
    }
    return out
  }

  /**
   * The [MAX_PALETTE_SIZE] most frequent colours.
   *
   * Ordering by frequency is load bearing: it guarantees the colours that get dropped are the rare ones. That suits
   * previews, which are flat team colours plus a thin tail of antialiasing. An image with an even colour spread would
   * quantise badly here.
   */
  private fun buildPalette(opaque: IntArray): IntArray {
    // IntIntMap rather than HashMap<Int, Int>: the latter is HashMap<Integer, Integer>, and since packed colours are
    // far outside the Integer cache every one of the million or so pixels allocated two or three boxes. That was 61%
    // of the whole application's allocation in a profile.
    val frequencies = IntIntMap(INITIAL_COLOUR_CAPACITY)
    for (colour in opaque) {
      frequencies.getAndIncrement(colour, 0, 1)
    }

    // Pack the count above the colour so the sort is a primitive LongArray sort instead of a boxed comparator over
    // map entries. Colours are 24 bit and never negative, so the count always dominates the ordering.
    val packed = LongArray(frequencies.size)
    var next = 0
    for (entry in frequencies) {
      packed[next++] = (entry.value.toLong() shl Int.SIZE_BITS) or entry.key.toLong()
    }
    packed.sort()

    val size = minOf(MAX_PALETTE_SIZE, packed.size)
    return IntArray(size) { packed[packed.size - 1 - it].toInt() }
  }

  /**
   * One map serves as both the exact lookup and the cache of approximated colours, so the nearest neighbour search
   * runs once per distinct colour rather than once per pixel.
   */
  private fun mapToPaletteIndices(opaque: IntArray, palette: IntArray): ByteArray {
    val lookup = IntIntMap(palette.size * 2)
    for (index in palette.indices) {
      lookup.put(palette[index], index)
    }

    val indices = ByteArray(opaque.size)
    for (pixel in opaque.indices) {
      val colour = opaque[pixel]
      var index = lookup.get(colour, NOT_IN_PALETTE)
      if (index == NOT_IN_PALETTE) {
        index = nearestPaletteIndex(colour, palette)
        lookup.put(colour, index)
      }
      indices[pixel] = index.toByte()
    }
    return indices
  }

  private fun nearestPaletteIndex(colour: Int, palette: IntArray): Int {
    val red = colour ushr 16 and 0xFF
    val green = colour ushr 8 and 0xFF
    val blue = colour and 0xFF

    var best = 0
    var bestDistance = Int.MAX_VALUE
    for (index in palette.indices) {
      val candidate = palette[index]
      val deltaRed = (candidate ushr 16 and 0xFF) - red
      val deltaGreen = (candidate ushr 8 and 0xFF) - green
      val deltaBlue = (candidate and 0xFF) - blue
      val distance = deltaRed * deltaRed + deltaGreen * deltaGreen + deltaBlue * deltaBlue
      if (distance < bestDistance) {
        bestDistance = distance
        best = index
        if (distance == 0) break
      }
    }
    return best
  }

  private fun buildPng(width: Int, height: Int, indices: ByteArray, palette: IntArray): ByteArray {
    val png = ByteArrayOutputStream(indices.size / 2)
    png.write(PNG_SIGNATURE)
    writeChunk(png, "IHDR", header(width, height))
    writeChunk(png, "PLTE", paletteChunk(palette))
    writeChunk(png, "IDAT", deflate(addFilterBytes(width, height, indices)))
    writeChunk(png, "IEND", ByteArray(0))
    return png.toByteArray()
  }

  private fun header(width: Int, height: Int): ByteArray {
    val header = ByteArrayOutputStream(13)
    writeInt(header, width)
    writeInt(header, height)
    header.write(BIT_DEPTH)
    header.write(COLOUR_TYPE_INDEXED)
    header.write(0) // compression, deflate is the only defined value
    header.write(0) // filter, adaptive is the only defined value
    header.write(0) // interlace, none
    return header.toByteArray()
  }

  private fun paletteChunk(palette: IntArray): ByteArray {
    val bytes = ByteArray(palette.size * 3)
    for (index in palette.indices) {
      val colour = palette[index]
      bytes[index * 3] = (colour ushr 16 and 0xFF).toByte()
      bytes[index * 3 + 1] = (colour ushr 8 and 0xFF).toByte()
      bytes[index * 3 + 2] = (colour and 0xFF).toByte()
    }
    return bytes
  }

  /**
   * Prefix every scanline with [FILTER_NONE].
   *
   * PNG allows a different filter per row, but for palette images every alternative is worse. Filters exist to turn
   * smoothly varying channel samples into small residuals; palette indices are labels, so subtracting one from
   * another is meaningless. It also actively hurts: two identical rows are byte for byte identical, which deflate
   * encodes as one long match, and filtering rewrites them into residuals that break that match up.
   *
   * Measured across the bundled previews, None beats the specification's adaptive minimum-sum heuristic by 13%, Sub
   * by 15% and Up by 12%. libpng gives the same advice for palette and low bit depth images.
   */
  private fun addFilterBytes(width: Int, height: Int, indices: ByteArray): ByteArray {
    val out = ByteArray(height * (width + 1))
    for (row in 0 until height) {
      val target = row * (width + 1)
      out[target] = FILTER_NONE.toByte()
      indices.copyInto(out, target + 1, row * width, (row + 1) * width)
    }
    return out
  }

  /**
   * PNG wraps its deflate stream in a zlib container. The single argument [Deflater] constructor produces exactly
   * that. Passing `nowrap = true` would emit raw deflate instead and every decoder would reject the file.
   */
  private fun deflate(raw: ByteArray): ByteArray {
    val deflater = Deflater(Deflater.BEST_COMPRESSION)
    try {
      deflater.setInput(raw)
      deflater.finish()
      val out = ByteArrayOutputStream(raw.size / 4)
      val buffer = ByteArray(DEFLATE_BUFFER_SIZE)
      while (!deflater.finished()) {
        out.write(buffer, 0, deflater.deflate(buffer))
      }
      return out.toByteArray()
    } finally {
      deflater.end()
    }
  }

  /** The chunk CRC covers the type and the data, but never the length. */
  private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
    val typeBytes = ByteArray(type.length) { type[it].code.toByte() }
    writeInt(out, data.size)
    out.write(typeBytes)
    out.write(data)

    val crc = CRC32()
    crc.update(typeBytes)
    crc.update(data)
    writeInt(out, crc.value.toInt())
  }

  /** PNG integers are big endian. */
  private fun writeInt(out: ByteArrayOutputStream, value: Int) {
    out.write(value ushr 24 and 0xFF)
    out.write(value ushr 16 and 0xFF)
    out.write(value ushr 8 and 0xFF)
    out.write(value and 0xFF)
  }
}