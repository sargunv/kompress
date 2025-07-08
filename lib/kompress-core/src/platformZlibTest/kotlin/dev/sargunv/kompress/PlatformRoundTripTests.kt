package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.Level
import kotlin.test.Test
import kotlinx.io.bytestring.encodeToByteString

class PlatformRoundTripTests {

  @Test
  fun simple() {
    testFixtureRoundTrip("1234567890".encodeToByteString(), ::PlatformDeflater, ::PlatformInflater)
  }

  @Test
  fun defaultOptions() {
    decompressedSamples.forEach { name ->
      testFixtureRoundTrip(name, ::PlatformDeflater, ::PlatformInflater)
    }
  }

  @Test
  fun nowrapOption() {
    decompressedSamples.forEach { name ->
      testFixtureRoundTrip(
        name,
        { PlatformDeflater(format = Format.Raw) },
        { PlatformInflater(format = Format.Raw) },
      )
    }
  }

  private fun levelTest(level: Int) {
    decompressedSamples.forEach { name ->
      testFixtureRoundTrip(
        name,
        { PlatformDeflater(level = level, format = Format.Raw) },
        { PlatformInflater(format = Format.Raw) },
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
  // TODO dictionaries
}
