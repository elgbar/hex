package no.elg.hex.util

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

fun compressAndReturnB64(text: String): String {
  val compress = compress(text.toByteArray(UTF_8))
  return String(Base64.getEncoder().encode(compress))
}

fun decompressB64(b64Compressed: String): String {
  val decompressedBArray = decompress(Base64.getDecoder().decode(b64Compressed))
  return String(decompressedBArray, UTF_8)
}

fun compress(bArray: ByteArray): ByteArray =
  ByteArrayOutputStream().apply {
    XZCompressorOutputStream(this).use { it.write(bArray) }
  }.toByteArray()

fun decompress(compressedTxt: ByteArray): ByteArray =
  XZCompressorInputStream(compressedTxt.inputStream()).use { it.readBytes() }