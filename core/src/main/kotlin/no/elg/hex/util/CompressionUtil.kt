package no.elg.hex.util

import com.badlogic.gdx.Gdx
import no.elg.hex.Settings
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.SingleXZInputStream
import org.tukaani.xz.XZOutputStream
import sheepy.util.text.Base85
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.text.Charsets.US_ASCII

fun encodeB85(compressed: ByteArray): String {
  val encoded = encodeB85ToBytes(compressed)
  return String(encoded, US_ASCII)
}
fun encodeB85ToBytes(compressed: ByteArray): ByteArray = Base85.getZ85Encoder().encode(compressed)

fun compressXZAndEncodeB85(text: String): String? {
  val compressed = compressXZ(text.toByteArray(UTF_8)) ?: return null
  return encodeB85(compressed)
}

fun compressXZAndEncodeB85(text: ByteArray): String? {
  val compressed = compressXZ(text) ?: return null
  return encodeB85(compressed)
}

/**
 * Decompresses a Base85 encoded string if it is valid, otherwise returns the original string.
 *
 * @param maybeCompressed The Base85 compressed string to decompress.
 * @return The decompressed string if valid, otherwise the original string.
 */
fun tryDecompressB85AndDecompressXZ(maybeCompressed: String): String? {
  val data = maybeCompressed.trim().toByteArray(US_ASCII)
  val decompressed = tryDecompressB85AndDecompressXZ(data) ?: return null
  return String(decompressed, UTF_8)
}

fun tryDecompressB85AndDecompressXZ(maybeCompressed: ByteArray): ByteArray? {
  return if (Base85.getZ85Decoder().test(maybeCompressed)) {
    val decoded = Base85.getZ85Decoder().decode(maybeCompressed)
    val decompressed = decompressXZ(decoded) ?: return null
    decompressed
  } else {
    maybeCompressed
  }
}

fun compressXZ(bArray: ByteArray): ByteArray? =
  try {
    ByteArrayOutputStream().apply {
      XZOutputStream(this, LZMA2Options(Settings.compressionPreset)).use { it.write(bArray) }
    }.toByteArray()
  } catch (e: Exception) {
    Gdx.app.error("compress util", "Failed to compress byte array", e)
    null
  }

fun decompressXZ(compressedTxt: ByteArray): ByteArray? =
  try {
    SingleXZInputStream(compressedTxt.inputStream()).use { it.readBytes() }
  } catch (e: Exception) {
    Gdx.app.error("decompress util", "Failed to decompress byte array", e)
    null
  }