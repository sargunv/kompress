@file:Suppress("LocalVariableName", "FunctionName")

package dev.sargunv.kompress.zlib

import kotlin.ushortArrayOf

private const val MAXBITS = WindowBits.MAX_WBITS

internal fun inflate_table(
  type: CodeType,
  lens: UShortArray,
  lens_index: UInt,
  codes: UInt,
  table: CodeArray,
  table_index: UInt,
  work: UShortArray,
  bits: UInt,
  setBits: (UInt) -> Unit,
): Int {
  var len: UInt // a code's length in bits
  var min = 0u
  var max = 0u // minimum and maximum code lengths
  var root = bits // number of index bits for root table
  var curr: UInt // number of index bits for current table
  var drop: UInt // code bits to drop for sub-table
  var left: Int // number of prefix codes available
  var huff: UInt // Huffman code
  var base: UShortArray? // base value table to use
  var match: UInt // use base and extra for symbol >= match
  val count = UShortArray(MAXBITS + 1) // number of codes of each length
  val offs = UShortArray(MAXBITS + 1) // offsets in table for each length
  var extra: UShortArray?

  // accumulate lengths for codes (assumes lens[] all in 0..MAXBITS)
  for (i in 0..MAXBITS) {
    count[i] = 0u
  }
  for (i in 0u until codes) {
    count[lens[lens_index.toInt() + i.toInt()].toInt()]++
  }

  // bound code lengths, force root to be within code lengths
  for (i in MAXBITS downTo 1) {
    if (count[i].toUInt() != 0u) {
      max = i.toUInt()
      break
    }
  }

  if (root > max) {
    root = max
  }

  if (max == 0u) { // no symbols to code at all
    table[table_index.toInt()] = Code(op = 64u, bits = 1u, value = 0u)
    table[table_index.toInt() + 1] = Code(op = 64u, bits = 1u, value = 0u)
    setBits(1u)
    return 0
  }

  for (i in 1..max.toInt()) {
    if (count[i].toUInt() != 0u) {
      min = i.toUInt()
      break
    }
  }

  if (root < min) {
    root = min
  }

  // check for an over-subscribed or incomplete set of lengths
  left = 1
  for (len in 1..MAXBITS) {
    left = left shl 1
    left -= count[len].toInt()
    if (left < 0) return -1 // over-subscribed
  }
  if (left > 0 && (type == CodeType.CODES || max != 1u)) {
    return -1 // incomplete set
  }

  // generate offsets into symbol table for each length for sorting
  offs[1] = 0u
  for (len in 1 until MAXBITS) {
    offs[len + 1] = (offs[len] + count[len]).toUShort()
  }

  // sort symbols by length, by symbol order within each length
  for (sym in 0u until codes) {
    if (lens[lens_index.toInt() + sym.toInt()].toUInt() != 0u) {
      val offset = offs[lens[lens_index.toInt() + sym.toInt()].toInt()].toInt()
      work[offset] = sym.toUShort()
      offs[lens[lens_index.toInt() + sym.toInt()].toInt()]++
    }
  }

  // set up for code type
  when (type) {
    CodeType.CODES -> {
      base = work
      extra = null /* dummy value--not used */
      match = 20u
    }
    CodeType.LENS -> {
      base = lbase
      extra = lext
      match = 257u
    }
    CodeType.DISTS -> {
      base = dbase
      extra = dext
      match = 0u
    }
  }

  // initialize state for loop
  huff = 0u // starting code
  var sym = 0u // starting code symbol
  // index of code symbols
  len = min // starting code length
  var next: UInt = table_index // current table to fill in
  // next available space in table
  curr = root // current table index bits
  drop = 0u // current bits to drop from code for index
  var low = -1 // trigger new sub-table when len > root
  var used: UInt = 1u shl root.toInt() // use root table entries
  // code entries in table used
  val mask = used - 1u // mask for comparing low

  // check available table space
  if (
    (type == CodeType.LENS && used > ENOUGH_LENS) || (type == CodeType.DISTS && used > ENOUGH_DISTS)
  ) {
    return 1
  }

  // process all codes and make table entries
  while (true) {
    // create table entry
    val here_bits: UByte = (len - drop).toUByte()
    val work_sym = work[sym.toInt()].toInt()

    val (here_op, here_val) =
      when {
        work_sym + 1 < match.toInt() -> Pair(0u, work_sym.toUInt())
        work_sym >= match.toInt() -> {
          val idx = work_sym - match.toInt()
          Pair(extra!![idx].toUInt(), base[idx].toUInt())
        }
        else -> Pair(32u + 64u, 0u) // end of block
      }

    // replicate for those indices with low len bits equal to huff
    var incr = 1u shl (len - drop).toInt()
    var fill = 1u shl curr.toInt()
    val min = fill // save offset to next table
    do {
      fill -= incr
      table[(next + (huff shr drop.toInt()) + fill).toInt()] =
        Code(op = here_op.toUByte(), bits = here_bits, value = here_val.toUShort())
    } while (fill != 0u)

    // backwards increment the len-bit code huff
    incr = 1u shl (len - 1u).toInt()
    while (huff and incr != 0u) {
      incr = incr shr 1
    }

    huff = (if (incr != 0u) (huff and (incr - 1u)) + incr else 0u)

    // go to next symbol, update count, len
    sym++
    count[len.toInt()] = (count[len.toInt()] - 1u).toUShort()
    if (count[len.toInt()] == 0u.toUShort()) {
      if (len == max) break
      len = lens[lens_index.toInt() + work[sym.toInt()].toInt()].toUInt()
    }

    // create new sub-table if needed
    if (len > root && (huff and mask) != low.toUInt()) {
      // if first time, transition to sub-tables
      if (drop == 0u) {
        drop = root
      }

      // increment past last table
      next += min // here min is 1 << curr

      // determine length of next table
      curr = len - drop
      left = 1 shl curr.toInt()
      while (curr + drop < max) {
        left -= count[(curr + drop).toInt()].toInt()
        if (left <= 0) break
        curr++
        left = left shl 1
      }

      // check for enough space
      used += 1u shl curr.toInt()
      if (
        (type == CodeType.LENS && used > ENOUGH_LENS) ||
          (type == CodeType.DISTS && used > ENOUGH_DISTS)
      ) {
        return 1
      }

      // point entry in root table to sub-table
      low = (huff and mask).toInt()
      table[low] =
        Code(op = curr.toUByte(), bits = root.toUByte(), value = (next - table_index).toUShort())
    }
  }

  // fill in remaining table entry if code is incomplete
  if (huff != 0u) {
    table[(next + huff).toInt()] = Code(op = 64u, bits = (len - drop).toUByte(), value = 0u)
  }

  // set return parameters
  setBits(root)
  return 0
}

private val lbase =
  ushortArrayOf(
    3u,
    4u,
    5u,
    6u,
    7u,
    8u,
    9u,
    10u,
    11u,
    13u,
    15u,
    17u,
    19u,
    23u,
    27u,
    31u,
    35u,
    43u,
    51u,
    59u,
    67u,
    83u,
    99u,
    115u,
    131u,
    163u,
    195u,
    227u,
    258u,
    0u,
    0u,
  )

private val lext =
  ushortArrayOf(
    16u,
    16u,
    16u,
    16u,
    16u,
    16u,
    16u,
    16u,
    17u,
    17u,
    17u,
    17u,
    18u,
    18u,
    18u,
    18u,
    19u,
    19u,
    19u,
    19u,
    20u,
    20u,
    20u,
    20u,
    21u,
    21u,
    21u,
    21u,
    16u,
    72u,
    78u,
  )

private val dbase =
  ushortArrayOf(
    1u,
    2u,
    3u,
    4u,
    5u,
    7u,
    9u,
    13u,
    17u,
    25u,
    33u,
    49u,
    65u,
    97u,
    129u,
    193u,
    257u,
    385u,
    513u,
    769u,
    1025u,
    1537u,
    2049u,
    3073u,
    4097u,
    6145u,
    8193u,
    12289u,
    16385u,
    24577u,
    0u,
    0u,
  )

private val dext =
  ushortArrayOf(
    16u,
    16u,
    16u,
    16u,
    17u,
    17u,
    18u,
    18u,
    19u,
    19u,
    20u,
    20u,
    21u,
    21u,
    22u,
    22u,
    23u,
    23u,
    24u,
    24u,
    25u,
    25u,
    26u,
    26u,
    27u,
    27u,
    28u,
    28u,
    29u,
    29u,
    64u,
    64u,
  )
