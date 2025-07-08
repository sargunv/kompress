package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.ReturnCode
import kotlin.test.Test

class InflateStatesTests {
  @Test
  fun testBadGzipMethod() {
    testInflate("1f 8b 0 0", 31, ReturnCode.Z_DATA_ERROR, "unknown compression method")
  }

  @Test
  fun testBadGzipFlags() {
    testInflate("1f 8b 8 80", 31, ReturnCode.Z_DATA_ERROR, "unknown header flags set")
  }

  @Test
  fun testBadZlibMethod() {
    testInflate("77 85", 15, ReturnCode.Z_DATA_ERROR, "unknown compression method")
  }

  @Test
  fun testSetWindowSizeFromHeader() {
    testInflate("8 99", 0, ReturnCode.Z_OK)
  }

  @Test
  fun testBadZlibWindowSize() {
    testInflate("78 9c", 8, ReturnCode.Z_DATA_ERROR, "invalid window size")
  }

  @Test
  fun testCheckAdler32() {
    testInflate("78 9c 63 0 0 0 1 0 1", 15, ReturnCode.Z_OK)
  }

  @Test
  fun testBadHeaderCrc() {
    testInflate(
      "1f 8b 8 1e 0 0 0 0 0 0 1 0 0 0 0 0 0",
      47,
      ReturnCode.Z_DATA_ERROR,
      "header crc mismatch",
    )
  }

  @Test
  fun testCheckGzipLength() {
    testInflate("1f 8b 8 2 0 0 0 0 0 0 1d 26 3 0 0 0 0 0 0 0 0 0", 47, ReturnCode.Z_OK)
  }

  @Test
  fun testBadZlibHeaderCheck() {
    testInflate("78 90", 47, ReturnCode.Z_DATA_ERROR, "incorrect header check")
  }

  @Test
  fun testNeedDictionary() {
    testInflate("8 b8 0 0 0 1", 8, ReturnCode.Z_NEED_DICT, "need dictionary")
  }

  @Test
  fun testComputeAdler32() {
    testInflate("78 9c 63 0", 15, ReturnCode.Z_OK)
  }
}
