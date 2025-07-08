package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.WindowBits

public enum class Format(
  /** The zlib windowBits value that corresponds to this data format. One of: 15, -15, 31, 47. */
  public val windowBits: Int
) {
  /** Write or process a zlib header and trailer around the compressed data. */
  Zlib(WindowBits.MAX_WBITS),

  /**
   * Write or process raw deflate data, not looking for a zlib or gzip header, not generating a
   * check value, and not looking for any check values for comparison at the end of the stream.
   */
  Raw(-WindowBits.MAX_WBITS),

  /** Write or process a gzip header and trailer around the compressed data. */
  Gzip(WindowBits.MAX_WBITS + 16),

  /**
   * During decompression, detect a gzip or zlib header and trailer around the compressed data.
   * During compression, this mode behaves the same as [Zlib].
   */
  Auto(WindowBits.MAX_WBITS + 32),
}
