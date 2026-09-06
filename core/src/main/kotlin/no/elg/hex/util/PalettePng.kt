package no.elg.hex.util

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Encodes RGBA8888 pixels as an 8 bit palette PNG (colour type 3).
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
  private const val FILTER_SUB = 1
  private const val FILTER_UP = 2

  private const val DEFLATE_BUFFER_SIZE = 1 shl 16

  /**
   * Encode [rgba], which must hold [width] * [height] pixels in RGBA8888 order.
   *
   * @param backgroundRgb Packed `0xRRGGBB` colour that translucent pixels are composited onto. Pass the colour the
   * preview is actually drawn against, otherwise antialiased edges pick up a fringe.
   */
  fun encode(width: Int, height: Int, rgba: ByteArray, backgroundRgb: Int): ByteArray {
    require(width > 0 && height > 0) { "Image must not be empty, got ${width}x$height" }
    val expected = width * height * BYTES_PER_RGBA_PIXEL
    require(rgba.size == expected) { "Expected $expected bytes of RGBA8888 data for a ${width}x$height image, got ${rgba.size}" }

    val opaque = compositeOntoBackground(rgba, backgroundRgb)
    val palette = buildPalette(opaque)
    val indices = mapToPaletteIndices(opaque, palette)

    return buildPng(width, height, indices, palette)
  }

  /**
   * Flatten RGBA pixels onto an opaque background, returning one packed `0xRRGGBB` value per pixel. Dropping alpha is
   * what lets the palette hold colours rather than colour/alpha pairs.
   */
  private fun compositeOntoBackground(rgba: ByteArray, backgroundRgb: Int): IntArray {
    val backgroundRed = backgroundRgb ushr 16 and 0xFF
    val backgroundGreen = backgroundRgb ushr 8 and 0xFF
    val backgroundBlue = backgroundRgb and 0xFF

    val out = IntArray(rgba.size / BYTES_PER_RGBA_PIXEL)
    for (pixel in out.indices) {
      val offset = pixel * BYTES_PER_RGBA_PIXEL
      var red = rgba[offset].toInt() and 0xFF
      var green = rgba[offset + 1].toInt() and 0xFF
      var blue = rgba[offset + 2].toInt() and 0xFF
      val alpha = rgba[offset + 3].toInt() and 0xFF

      if (alpha != 0xFF) {
        // + 127 rounds the blend instead of truncating it
        val inverse = 0xFF - alpha
        red = (red * alpha + backgroundRed * inverse + 127) / 0xFF
        green = (green * alpha + backgroundGreen * inverse + 127) / 0xFF
        blue = (blue * alpha + backgroundBlue * inverse + 127) / 0xFF
      }
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
    val frequencies = HashMap<Int, Int>()
    for (colour in opaque) {
      frequencies[colour] = (frequencies[colour] ?: 0) + 1
    }
    return frequencies.entries
      .sortedByDescending { it.value }
      .take(MAX_PALETTE_SIZE)
      .map { it.key }
      .toIntArray()
  }

  /** Cached per distinct colour rather than per pixel, so the nearest neighbour search runs at most a few thousand times. */
  private fun mapToPaletteIndices(opaque: IntArray, palette: IntArray): ByteArray {
    val exact = HashMap<Int, Int>(palette.size * 2)
    for (index in palette.indices) {
      exact[palette[index]] = index
    }
    val approximated = HashMap<Int, Int>()

    val indices = ByteArray(opaque.size)
    for (pixel in opaque.indices) {
      val colour = opaque[pixel]
      val index = exact[colour] ?: approximated.getOrPut(colour) { nearestPaletteIndex(colour, palette) }
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
    writeChunk(png, "IDAT", deflate(filterScanlines(width, height, indices)))
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
   * Prefix every scanline with the filter that minimises the sum of absolute differences, the heuristic from the PNG
   * specification.
   *
   * Only [FILTER_NONE], [FILTER_SUB] and [FILTER_UP] are candidates. The remaining two, Average and Paeth, interpolate
   * between byte values, which assumes those bytes lie on a continuum. Palette indices do not: entry 7 and entry 200
   * are labels, so averaging them produces noise and compresses worse.
   */
  private fun filterScanlines(width: Int, height: Int, indices: ByteArray): ByteArray {
    val out = ByteArray(height * (width + 1))
    val sub = ByteArray(width)
    val up = ByteArray(width)
    var previous = ByteArray(width)

    for (row in 0 until height) {
      val start = row * width
      val line = indices.copyOfRange(start, start + width)

      sub[0] = line[0]
      for (column in 1 until width) {
        sub[column] = (line[column] - line[column - 1]).toByte()
      }
      for (column in 0 until width) {
        up[column] = (line[column] - previous[column]).toByte()
      }

      val noneScore = absoluteSum(line)
      val subScore = absoluteSum(sub)
      val upScore = absoluteSum(up)

      val target = row * (width + 1)
      when {
        noneScore <= subScore && noneScore <= upScore -> {
          out[target] = FILTER_NONE.toByte()
          line.copyInto(out, target + 1)
        }

        subScore <= upScore -> {
          out[target] = FILTER_SUB.toByte()
          sub.copyInto(out, target + 1)
        }

        else -> {
          out[target] = FILTER_UP.toByte()
          up.copyInto(out, target + 1)
        }
      }
      previous = line
    }
    return out
  }

  /** Sum of the bytes read as signed values, which is how the specification scores a candidate filter. */
  private fun absoluteSum(line: ByteArray): Long {
    var sum = 0L
    for (byte in line) {
      val unsigned = byte.toInt() and 0xFF
      sum += if (unsigned < 128) unsigned else 256 - unsigned
    }
    return sum
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