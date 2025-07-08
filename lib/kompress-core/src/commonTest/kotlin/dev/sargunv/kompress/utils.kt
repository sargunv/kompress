package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.InflateStream
import dev.sargunv.kompress.zlib.ReturnCode
import dev.sargunv.kompress.zlib.ReturnCode.*
import dev.sargunv.kompress.zlib.inflate
import dev.sargunv.kompress.zlib.inflateInit2
import dev.sargunv.kompress.zlib.inflateReset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.io.buffered
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteString

private fun hex2Bytes(hex: String): UByteArray {
  return if (hex.isBlank()) UByteArray(0)
  else hex.split(' ').map { it.toInt(16).toUByte() }.toUByteArray()
}

internal fun testInflate(hex: String, wbits: Int, status: ReturnCode, msg: String? = null) {
  fun push() =
    ZStreamProcessor(
        new = { InflateStream() },
        init = { inflateInit2(windowBits = wbits) },
        process = { inflate(it) },
        reset = { inflateReset() },
      )
      .use { inflater ->
        val bytes = hex2Bytes(hex)
        inflater.push(bytes, 0, bytes.size, false) { _, _, _ -> }
      }

  when (status) {
    Z_OK,
    Z_STREAM_END -> {
      require(msg == null)
      push()
      // TODO: assertEquals(status, if (push()) Z_STREAM_END else Z_OK)
    }
    else -> {
      require(msg != null)
      val cause = assertFailsWith<ZStreamException> { push() }
      assertEquals(status, cause.code)
      assertEquals(msg, cause.message)
    }
  }
}

val decompressedSamples =
  listOf(
      "blank.gif",
      "lorem.txt",
      "utf8.zip",
      "lorem_cat.jpeg",
      "lorem_en_100k.txt",
      "lorem_utf_100k.txt",
    )
    .map { "samples/$it" }

val compressedSamples =
  listOf(
      "KW_Rocketry_1.compressed",
      "shapefile.compressed",
      "sheet2.compressed",
      "sheet3.compressed",
      "sheet4.compressed",
    )
    .map { "samples_deflated_raw/$it" }

fun loadFixture(name: String): ByteString =
  SystemFileSystem.source(Path("../..", "test-fixtures", name)).buffered().readByteString()

fun assertEqualsHex(expected: ByteString, actual: ByteString) {
  val format = HexFormat {
    bytes {
      bytesPerLine = 16
      bytesPerGroup = 8
      byteSeparator = " "
    }
  }
  assertEquals(expected.toHexString(format), actual.toHexString(format))
}

val hexFormat = HexFormat {
  bytes {
    bytesPerLine = 16
    bytesPerGroup = 8
    byteSeparator = " "
  }
}

fun testFixtureRoundTrip(
  name: String,
  compressor: () -> Compressor,
  decompressor: () -> Decompressor,
) {
  println("Fixture: $name...")
  testFixtureRoundTrip(loadFixture(name), compressor, decompressor)
}

fun testFixtureRoundTrip(
  original: ByteString,
  compressor: () -> Compressor,
  decompressor: () -> Decompressor,
) {
  val small = 1024
  println("Original size: ${original.size}")
  if (original.size <= small) println(original.toHexString(hexFormat))
  val compressed = original.compressed(compressor())
  println("Compressed size: ${compressed.size}")
  if (compressed.size <= small) println(compressed.toHexString(hexFormat))
  val decompressed = compressed.decompressed(decompressor())
  println("Decompressed size: ${decompressed.size}")
  if (decompressed.size <= small) println(decompressed.toHexString(hexFormat))
  assertEqualsHex(original, decompressed)
  println("")
}
