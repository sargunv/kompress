package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.ReturnCode
import kotlin.test.Test

class InflateSupportTests {
  @Test
  fun testForceWindowAllocation() {
    testInflate("63 0", -15, ReturnCode.Z_OK)
  }

  @Test
  fun testForceWindowReplacement() {
    testInflate("63 18 5", -8, ReturnCode.Z_OK)
  }

  @Test
  fun testForceSplitWindowUpdate() {
    testInflate("63 18 68 30 d0 0 0", -8, ReturnCode.Z_OK)
  }

  @Test
  fun testUseFixedBlocks() {
    testInflate("3 0", -15, ReturnCode.Z_STREAM_END)
  }

  @Test
  fun testBadWindowSize() {
    testInflate("", 1, ReturnCode.Z_STREAM_ERROR, "stream error")
  }
}
