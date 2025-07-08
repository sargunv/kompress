package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.ReturnCode
import kotlin.test.Test

class InflateFastTests {
  @Test
  fun testFastLengthExtraBits() {
    testInflate(
      "e5 e0 81 ad 6d cb b2 2c c9 01 1e 59 63 ae 7d ee fb 4d fd b5 35 41 68 ff 7f 0f 0 0 0",
      -8,
      ReturnCode.Z_DATA_ERROR,
      "invalid distance too far back",
    )
  }

  @Test
  fun testFastDistanceExtraBits() {
    testInflate(
      "25 fd 81 b5 6d 59 b6 6a 49 ea af 35 6 34 eb 8c b9 f6 b9 1e ef 67 49 50 fe ff ff 3f 0 0",
      -8,
      ReturnCode.Z_DATA_ERROR,
      "invalid distance too far back",
    )
  }

  @Test
  fun testFastInvalidDistanceCode() {
    testInflate("3 7e 0 0 0 0 0", -8, ReturnCode.Z_DATA_ERROR, "invalid distance code")
  }

  @Test()
  fun testFastInvalidLiteralLengthCode() {
    testInflate("1b 7 0 0 0 0 0", -8, ReturnCode.Z_DATA_ERROR, "invalid literal/length code")
  }

  @Test
  fun testFast2ndLevelCodesAndTooFarBack() {
    testInflate(
      "d c7 1 ae eb 38 c 4 41 a0 87 72 de df fb 1f b8 36 b1 38 5d ff ff 0",
      -8,
      ReturnCode.Z_DATA_ERROR,
      "invalid distance too far back",
    )
  }

  @Test
  fun testVeryCommonCase() {
    testInflate("63 18 5 8c 10 8 0 0 0 0", -8, ReturnCode.Z_OK)
  }

  @Test
  fun testContiguousAndWrapAroundWindow() {
    testInflate("63 60 60 18 c9 0 8 18 18 18 26 c0 28 0 29 0 0 0", -8, ReturnCode.Z_OK)
  }

  @Test
  fun testCopyDirectFromOutput() {
    testInflate("63 0 3 0 0 0 0 0", -8, ReturnCode.Z_OK)
  }
}
