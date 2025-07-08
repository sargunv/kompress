package dev.sargunv.kompress.zlib

public const val CRC32_INIT: UInt = 0u

/**
 * Update a running CRC-32 with the bytes buf[0..len-1] and return the updated CRC-32. A CRC-32
 * value is in the range of a 32-bit unsigned integer. If buf is null, this function returns the
 * required initial value for the crc. Pre- and post-conditioning (one's complement) is performed
 * within this function so it shouldn't be done by the application.
 */
public fun crc32b(old: UInt, buf: UByteArray, len: Int, pos: Int): UInt {
  return (pos..<(pos + len))
    .fold(old.inv()) { crc, i ->
      val index = crc.toUByte() xor buf[i]
      (crc shr 8) xor table[index.toInt()]
    }
    .inv()
}

private val table =
  UIntArray(256) {
    (0..<8).fold(it.toUInt()) { crc, _ ->
      (if ((crc and 1u) != 0u) 0xEDB88320u xor (crc shr 1) else crc shr 1)
    }
  }
