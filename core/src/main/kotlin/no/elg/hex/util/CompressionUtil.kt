package no.elg.hex.util

import com.badlogic.gdx.Gdx
import org.apache.pdfbox.filter.ASCII85InputStream
import org.apache.pdfbox.filter.ASCII85OutputStream
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.SingleXZInputStream
import org.tukaani.xz.XZOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8

fun compressAndReturnB64(text: String): String? {
  val compress = compress(text.toByteArray(UTF_8)) ?: return null
  return String(compress, UTF_8)
}

fun decompressB64(b64Compressed: String): String? {
  val decompressedBArray = decompress(b64Compressed.toByteArray()) ?: return null
  return String(decompressedBArray, UTF_8)
}

fun compress(bArray: ByteArray): ByteArray? =
  try {
    ByteArrayOutputStream().apply {
      ASCII85OutputStream(this).also { it.lineLength = Int.MAX_VALUE }.run { XZOutputStream(this, LZMA2Options()).use { it.write(bArray) } }
    }.toByteArray()
  } catch (e: Exception) {
    Gdx.app.error("compress util", "Failed to compress byte array", e)
    null
  }

fun decompress(compressedTxt: ByteArray): ByteArray? =
  try {
    SingleXZInputStream(ASCII85InputStream(compressedTxt.inputStream())).use { it.readBytes() }
  } catch (e: Exception) {
    Gdx.app.error("decompress util", "Failed to decompress byte array", e)
    null
  }