package dev.sargunv.kompress.zlib

import kotlin.jvm.JvmInline

internal fun zmemcpy(
  dest: UByteArray,
  destStart: UInt,
  src: UByteArray,
  srcStart: UInt,
  len: UInt,
) {
  src.copyInto(
    destination = dest,
    destinationOffset = destStart.toInt(),
    startIndex = srcStart.toInt(),
    endIndex = (srcStart + len).toInt(),
  )
}

@JvmInline
@Suppress("NOTHING_TO_INLINE")
internal value class CodeArray(val codes: UIntArray) {
  constructor(size: Int) : this(UIntArray(size))

  inline operator fun get(index: Int) = Code(codes[index])

  inline operator fun set(index: Int, code: Code) {
    codes[index] = code.packed
  }
}

/**
 * Structure for decoding tables. Each entry provides either the information needed to do the
 * operation requested by the code that indexed that table entry, or it provides a pointer to
 * another table that indexes more bits of the code.
 */
@JvmInline
internal value class Code(val packed: UInt) {
  constructor(
    op: UByte,
    bits: UByte,
    value: UShort,
  ) : this((op.toUInt() shl 24) or (bits.toUInt() shl 16) or value.toUInt())

  /**
   * op indicates whether the entry is a pointer to another table, a literal, a length or distance,
   * an end-of-block, or an invalid code. For a table pointer, the low four bits of op is the number
   * of index bits of that table. For a length or distance, the low four bits of op is the number of
   * extra bits to get after the code.
   */
  val op: UByte
    inline get() = (packed shr 24).toUByte()

  /** bits is the number of bits in this code or part of the code to drop off of the bit buffer. */
  val bits: UByte
    inline get() = (packed shr 16).toUByte()

  /**
   * val is the actual byte to output in the case of a literal, the base length or distance, or the
   * offset from the current table to the next table. Each entry is four bytes.
   */
  val value: UShort
    inline get() = packed.toUShort()

  companion object {
    @Suppress("NOTHING_TO_INLINE")
    object Op {
      const val LITERAL = 0b00000000u
      const val END_OF_BLOCK = 0b01100000u
      const val INVALID_CODE = 0b01000000u

      /** table link, [tttt] != 0 is the number of table index bits */
      inline fun tableLink(tttt: UByte) = (tttt and 0b00001111u)

      /** length or distance, [eeee] is the number of extra bits */
      inline fun distance(eeee: UByte) = (eeee and 0b00001111u) or 0b00010000u
    }
  }
}
