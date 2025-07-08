package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.bytestring.toHexString

class InflateTests {

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

  @Test
  fun fromCompressed() {
    compressedSamples.forEach { name ->
      val data = loadFixture(name)
      val platformResult = data.decompressed(PlatformInflater(format = Format.Raw))
      val kotlinResult = data.decompressed(Inflater(format = Format.Raw))
      assertEqualsHex(platformResult, kotlinResult)
    }
  }

  @Test
  fun simple() {
    testFixtureRoundTrip("1234567890".encodeToByteString(), ::PlatformDeflater, ::Inflater)
  }

  @Test
  fun defaultOptions() {
    decompressedSamples.forEach { name ->
      testFixtureRoundTrip(name, ::PlatformDeflater, ::Inflater)
    }
  }

  @Test
  fun formatRaw() {
    decompressedSamples.forEach { name ->
      testFixtureRoundTrip(
        name,
        { PlatformDeflater(format = Format.Raw) },
        { Inflater(format = Format.Raw) },
      )
    }
  }

  private fun levelTest(level: Int) {
    decompressedSamples.forEach { name ->
      testFixtureRoundTrip(
        name,
        { PlatformDeflater(level = level, format = Format.Raw) },
        { Inflater(format = Format.Raw) },
      )
    }
  }

  @Test fun level0() = levelTest(Level.NO_COMPRESSION)

  @Test fun level1() = levelTest(Level.BEST_SPEED)

  @Test fun level2() = levelTest(2)

  @Test fun level3() = levelTest(3)

  @Test fun level4() = levelTest(4)

  @Test fun level5() = levelTest(5)

  @Test fun level6() = levelTest(6)

  @Test fun level7() = levelTest(7)

  @Test fun level8() = levelTest(8)

  @Test fun level9() = levelTest(Level.BEST_COMPRESSION)

  // TODO all strategies
  // TODO all windowbits
  // TODO dictionaries
}
