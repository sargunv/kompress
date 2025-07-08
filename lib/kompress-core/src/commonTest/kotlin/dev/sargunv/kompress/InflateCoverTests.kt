package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.ReturnCode
import kotlin.test.Test

class InflateCoverTests {
  @Test
  fun testInvalidStoredBlockLengths() {
    testInflate("0 0 0 0 0", -15, ReturnCode.Z_DATA_ERROR, "invalid stored block lengths")
  }

  @Test
  fun testFixed() {
    testInflate("3 0", -15, ReturnCode.Z_OK)
  }

  @Test
  fun testInvalidBlockType() {
    testInflate("6", -15, ReturnCode.Z_DATA_ERROR, "invalid block type")
  }

  @Test
  fun testStored() {
    testInflate("1 1 0 fe ff 0", -15, ReturnCode.Z_OK)
  }

  @Test
  fun testTooManyLengthOrDistanceSymbols() {
    testInflate("fc 0 0", -15, ReturnCode.Z_DATA_ERROR, "too many length or distance symbols")
  }

  @Test
  fun testInvalidCodeLengthsSet() {
    testInflate("4 0 fe ff", -15, ReturnCode.Z_DATA_ERROR, "invalid code lengths set")
  }

  @Test
  fun testInvalidBitLengthRepeat1() {
    testInflate("4 0 24 49 0", -15, ReturnCode.Z_DATA_ERROR, "invalid bit length repeat")
  }

  @Test
  fun testInvalidBitLengthRepeat2() {
    testInflate("4 0 24 e9 ff ff", -15, ReturnCode.Z_DATA_ERROR, "invalid bit length repeat")
  }

  @Test
  fun testInvalidCodeMissingEndOfBlock() {
    testInflate(
      "4 0 24 e9 ff 6d",
      -15,
      ReturnCode.Z_DATA_ERROR,
      "invalid code -- missing end-of-block",
    )
  }

  @Test
  fun testInvalidLiteralLengthsSet() {
    testInflate(
      "4 80 49 92 24 49 92 24 71 ff ff 93 11 0",
      -15,
      ReturnCode.Z_DATA_ERROR,
      "invalid literal/lengths set",
    )
  }

  @Test
  fun invalidDistancesSet() {
    testInflate(
      "4 80 49 92 24 49 92 24 f b4 ff ff c3 84",
      -15,
      ReturnCode.Z_DATA_ERROR,
      "invalid distances set",
    )
  }

  @Test
  fun testInvalidLiteralLengthCode() {
    testInflate(
      "4 80 49 92 24 49 92 24 f b4 ff ff c3 84",
      -15,
      ReturnCode.Z_DATA_ERROR,
      "invalid distances set",
    )
  }

  @Test
  fun testInvalidDistanceCode() {
    testInflate("2 7e ff ff", -15, ReturnCode.Z_DATA_ERROR, "invalid distance code")
  }

  @Test
  fun testInvalidDistanceTooFarBack() {
    testInflate(
      "c c0 81 0 0 0 0 0 90 ff 6b 4 0",
      -15,
      ReturnCode.Z_DATA_ERROR,
      "invalid distance too far back",
    )
  }

  @Test
  fun testIncorrectDataCheck() {
    testInflate(
      "1f 8b 8 0 0 0 0 0 0 0 3 0 0 0 0 1",
      47,
      ReturnCode.Z_DATA_ERROR,
      "incorrect data check",
    )
  }

  @Test
  fun testIncorrectLengthCheck() {
    testInflate(
      "1f 8b 8 0 0 0 0 0 0 0 3 0 0 0 0 0 0 0 0 1",
      47,
      ReturnCode.Z_DATA_ERROR,
      "incorrect length check",
    )
  }

  @Test
  fun testPull17() {
    testInflate("5 c0 21 d 0 0 0 80 b0 fe 6d 2f 91 6c", -15, ReturnCode.Z_OK)
  }

  @Test
  fun testLongCode() {
    testInflate(
      "5 e0 81 91 24 cb b2 2c 49 e2 f 2e 8b 9a 47 56 9f fb fe ec d2 ff 1f",
      -15,
      ReturnCode.Z_OK,
    )
  }

  @Test
  fun testLengthExtra() {
    testInflate("ed c0 1 1 0 0 0 40 20 ff 57 1b 42 2c 4f", -15, ReturnCode.Z_OK)
  }

  @Test
  fun testLongDistanceAndExtra() {
    testInflate(
      "ed cf c1 b1 2c 47 10 c4 30 fa 6f 35 1d 1 82 59 3d fb be 2e 2a fc f c",
      -15,
      ReturnCode.Z_OK,
    )
  }

  @Test
  fun testWindowEnd() {
    testInflate(
      "ed c0 81 0 0 0 0 80 a0 fd a9 17 a9 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 6",
      -15,
      ReturnCode.Z_OK,
    )
  }

  @Test
  fun testInflateFastTypeReturn() {
    testInflate("2 8 20 80 0 3 0", -15, ReturnCode.Z_OK)
  }

  @Test
  fun testWindowWrap() {
    testInflate("63 18 5 40 c 0", -8, ReturnCode.Z_OK)
  }
}
