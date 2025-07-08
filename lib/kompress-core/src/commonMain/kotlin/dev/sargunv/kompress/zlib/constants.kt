package dev.sargunv.kompress.zlib

public object Level {
  public const val NO_COMPRESSION: Int = 0
  public const val BEST_SPEED: Int = 1
  public const val BEST_COMPRESSION: Int = 9
  public const val DEFAULT_COMPRESSION: Int = 6
}

public object MemLevel {
  public const val MINIMUM_MEMORY: Int = 1
  public const val MAXIMUM_MEMORY: Int = 9
  public const val DEFAULT_MEMLEVEL: Int = 8
}

public object WindowBits {
  public const val MIN_WBITS: Int = 8
  public const val MAX_WBITS: Int = 15
  public const val DEF_WBITS: Int = MAX_WBITS
}

public enum class ReturnCode(public val value: Int, public val message: String) {
  Z_VERSION_ERROR(-6, "incompatible version"),
  Z_BUF_ERROR(-5, "buffer error"),
  Z_MEM_ERROR(-4, "insufficient memory"),
  Z_DATA_ERROR(-3, "data error"),
  Z_STREAM_ERROR(-2, "stream error"),
  Z_ERRNO(-1, "file error"),
  Z_OK(0, ""),
  Z_STREAM_END(1, "stream end"),
  Z_NEED_DICT(2, "need dictionary");

  public companion object {
    @Suppress("NOTHING_TO_INLINE")
    public inline fun fromValue(value: Int): ReturnCode? = entries.firstOrNull { it.value == value }
  }
}

public enum class Flush(public val value: UInt) {
  Z_NO_FLUSH(0u),
  Z_PARTIAL_FLUSH(1u),
  Z_SYNC_FLUSH(2u),
  Z_FULL_FLUSH(3u),
  Z_FINISH(4u),
  Z_BLOCK(5u),
  Z_TREES(6u);

  public companion object {
    @Suppress("NOTHING_TO_INLINE")
    public inline fun fromValue(value: UInt): Flush? = entries.firstOrNull { it.value == value }
  }
}

public enum class Strategy(public val value: UInt) {
  Z_DEFAULT_STRATEGY(0u),
  Z_FILTERED(1u),
  Z_HUFFMAN_ONLY(2u),
  Z_RLE(3u),
  Z_FIXED(4u);

  public companion object {
    @Suppress("NOTHING_TO_INLINE")
    public inline fun fromValue(value: UInt): Strategy? = entries.firstOrNull { it.value == value }
  }
}

public enum class DataType(public val value: UInt) {
  Z_BINARY(0u),
  Z_TEXT(1u),
  Z_UNKNOWN(2u);

  public companion object {
    @Suppress("NOTHING_TO_INLINE")
    public inline fun fromValue(value: UInt): DataType? = entries.firstOrNull { it.value == value }
  }
}

public enum class Method(public val value: UByte) {
  Z_DEFLATED(8u);

  public companion object {
    @Suppress("NOTHING_TO_INLINE")
    public inline fun fromValue(value: UByte): Method? = entries.firstOrNull { it.value == value }
  }
}

internal enum class CodeType(val value: Int) {
  CODES(0),
  LENS(1),
  DISTS(2),
}
