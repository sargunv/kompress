package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.CodeArray
import dev.sargunv.kompress.zlib.CodeType
import dev.sargunv.kompress.zlib.ENOUGH_DISTS
import dev.sargunv.kompress.zlib.inflate_table
import kotlin.test.Test
import kotlin.test.assertEquals

class InflateCoverTreesTests {
  @Test
  fun testInflateEmpty() {
    val table = CodeArray(ENOUGH_DISTS.toInt())
    val lens = UShortArray(16) { (it + 1).coerceAtMost(15).toUShort() }
    val work = UShortArray(16)

    var ret = inflate_table(CodeType.DISTS, lens, 0u, 16u, table, 0u, work, 15u) {}
    assertEquals(1, ret)

    ret = inflate_table(CodeType.DISTS, lens, 0u, 16u, table, 0u, work, 1u) {}
    assertEquals(1, ret)
  }
}
