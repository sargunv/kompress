package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.ADLER32_INIT
import dev.sargunv.kompress.zlib.CRC32_INIT
import dev.sargunv.kompress.zlib.adler32
import dev.sargunv.kompress.zlib.crc32b
import kotlin.test.Test
import kotlin.test.assertEquals

class ChecksumTest {
  @Test
  fun testCrc32() {
    val input = "123456789".encodeToByteArray()
    val crc = crc32b(old = CRC32_INIT, buf = input.toUByteArray(), len = input.size, pos = 0)
    assertEquals(0xcbf43926u.toString(16), crc.toString(16))
  }

  @Test
  fun testAdler32() {
    val input = "123456789".encodeToByteArray()
    val adler = adler32(old = ADLER32_INIT, buf = input.toUByteArray(), len = input.size, pos = 0)
    assertEquals(0x091e01deu, adler)
  }
}
