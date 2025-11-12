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

fun compressB85(text: String): String? {
  val compressed = compress(text.toByteArray(UTF_8)) ?: return null
  val encoded = Base85.getZ85Encoder().encode(compressed)
  return String(encoded, US_ASCII)
}

/**
 * Decompresses a Base85 encoded string if it is valid, otherwise returns the original string.
 *
 * @param b85Compressed The Base85 compressed string to decompress.
 * @return The decompressed string if valid, otherwise the original string.
 */
fun tryDecompressB85(b85Compressed: String): String? {
  val data = b85Compressed.trim().toByteArray(US_ASCII)
  return if (Base85.getZ85Decoder().test(data)) {
    val decoded = Base85.getZ85Decoder().decode(data)
    val decompressed = decompress(decoded) ?: return null
    String(decompressed, UTF_8)
  } else {
    b85Compressed
  }
}

fun compress(bArray: ByteArray): ByteArray? =
  try {
    ByteArrayOutputStream().apply {
      XZOutputStream(this, LZMA2Options(Settings.compressionPreset)).use { it.write(bArray) }
    }.toByteArray()
  } catch (e: Exception) {
    Gdx.app.error("compress util", "Failed to compress byte array", e)
    null
  }

fun decompress(compressedTxt: ByteArray): ByteArray? =
  try {
    SingleXZInputStream(compressedTxt.inputStream()).use { it.readBytes() }
  } catch (e: Exception) {
    Gdx.app.error("decompress util", "Failed to decompress byte array", e)
    null
  }