package no.elg.hex.util

import com.badlogic.gdx.Gdx
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

fun decompressB85(b64Compressed: String): String? {
  val decompressed = decompressB85ByteArray(b64Compressed) ?: return null
  return String(decompressed, UTF_8)
}

fun decompressB85ByteArray(b64Compressed: String): ByteArray? {
  val decoded = Base85.getZ85Decoder().decode(b64Compressed.toByteArray(US_ASCII))
  return decompress(decoded)
}

fun compress(bArray: ByteArray): ByteArray? =
  try {
    ByteArrayOutputStream().apply {
      XZOutputStream(this, LZMA2Options()).use { it.write(bArray) }
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