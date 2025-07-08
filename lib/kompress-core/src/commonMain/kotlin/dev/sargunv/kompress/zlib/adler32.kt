package dev.sargunv.kompress.zlib

public const val ADLER32_INIT: UInt = 1u

/**
 * Update a running Adler-32 checksum with the bytes buf[0..len-1] and return the updated checksum.
 * An Adler-32 value is in the range of a 32-bit unsigned integer. If buf is null, this function
 * returns the required initial value for the checksum.
 *
 * An Adler-32 checksum is almost as reliable as a CRC-32 but can be computed much faster.
 */
public fun adler32(old: UInt, buf: UByteArray, len: Int, pos: Int): UInt {
  var sum1 = (old and 0xffffu)
  var sum2 = ((old shr 16) and 0xffffu)

  var pos = pos
  var len = len

  while (len != 0) {
    // Set limit ~ twice less than 5552, to keep
    // s2 in 31-bits, because we force signed ints.
    // in other case %= will fail.
    var n = len.coerceAtMost(2000)
    len -= n

    do {
      sum1 = sum1 + buf[pos++]
      sum2 = sum2 + sum1
      n -= 1
    } while (n != 0)

    sum1 %= 65521u
    sum2 %= 65521u
  }

  return sum1 or (sum2 shl 16)
}
