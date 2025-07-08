@file:Suppress("FunctionName")

package dev.sargunv.kompress.zlib

import dev.sargunv.kompress.zlib.CodeType.*
import dev.sargunv.kompress.zlib.Flush.*
import dev.sargunv.kompress.zlib.InflateMode.*
import dev.sargunv.kompress.zlib.InflateState.Flags
import dev.sargunv.kompress.zlib.InflateState.Wrap
import dev.sargunv.kompress.zlib.Method.*
import dev.sargunv.kompress.zlib.ReturnCode.*
import dev.sargunv.kompress.zlib.ZStream.State
import kotlin.jvm.JvmInline
import kotlin.time.Instant
import kotlinx.io.bytestring.ByteStringBuilder
import kotlinx.io.bytestring.append

public typealias InflateStream = ZStream<InflateState>

internal enum class InflateMode(val value: Int) {
  /** i: waiting for magic header */
  HEAD(16180),
  /** i: waiting for method and flags (gzip) */
  FLAGS(16181),
  /** i: waiting for modification time (gzip) */
  TIME(16182),
  /** i: waiting for extra flags and operating system (gzip) */
  OS(16183),
  /** i: waiting for extra length (gzip) */
  EXLEN(16184),
  /** i: waiting for extra bytes (gzip) */
  EXTRA(16185),
  /** i: waiting for end of file name (gzip) */
  NAME(16186),
  /** i: waiting for end of comment (gzip) */
  COMMENT(16187),
  /** i: waiting for header crc (gzip) */
  HCRC(16188),
  /** i: waiting for dictionary check value */
  DICTID(16189),
  /** waiting for inflateSetDictionary() call */
  DICT(16190),
  /** i: waiting for type bits, including last-flag bit */
  TYPE(16191),
  /** i: same, but skip check to exit inflate on new block */
  TYPEDO(16192),
  /** i: waiting for stored size (length and complement) */
  STORED(16193),
  /** i/o: same as COPY below, but only first time in */
  COPY_(16194),
  /** i/o: waiting for input or output to copy stored block */
  COPY(16195),
  /** i: waiting for dynamic block table lengths */
  TABLE(16196),
  /** i: waiting for code length code lengths */
  LENLENS(16197),
  /** i: waiting for length/lit and distance code lengths */
  CODELENS(16198),
  /** i: same as LEN below, but only first time in */
  LEN_(16199),
  /** i: waiting for length/lit/eob code */
  LEN(16200),
  /** i: waiting for length extra bits */
  LENEXT(16201),
  /** i: waiting for distance code */
  DIST(16202),
  /** i: waiting for distance extra bits */
  DISTEXT(16203),
  /** o: waiting for output space to copy string */
  MATCH(16204),
  /** o: waiting for output space to write literal */
  LIT(16205),
  /** i: waiting for 32-bit check value */
  CHECK(16206),
  /** i: waiting for 32-bit length (gzip) */
  LENGTH(16207),
  /** finished check, done -- remain here until reset */
  DONE(16208),
  /** got a data error -- remain here until reset */
  BAD(16209),
  /** got an inflate() memory error -- remain here until reset */
  MEM(16210),
  /** looking for synchronization bytes to restart inflate() */
  SYNC(16211),
}

internal const val ENOUGH_LENS = 852u

internal const val ENOUGH_DISTS = 592u

private fun zswap32(q: UInt): UInt {
  //  val q = 0xAABBCCDDu
  val a = ((q shr 24) and 0xffu) //  0x000000AAu
  val b = ((q shr 8) and 0xff00u) // 0x0000BB00u
  val c = ((q and 0xff00u) shl 8) // 0x00CC0000u
  val d = ((q and 0xffu) shl 24) //  0xDD000000u
  return (a or b or c or d) // 0xDDCCBBAAu
}

public class InflateState : State {
  /** Pointer back to this zlib stream */
  internal var strm: InflateStream? = null

  /** Current inflate mode */
  internal var mode: InflateMode = HEAD

  /** True if processing last block */
  internal var last: Boolean = false

  /** Bit 0 true for zlib, bit 1 true for gzip, bit 2 true to validate check value */
  internal var wrap: Wrap = Wrap()

  @JvmInline
  internal value class Wrap(val value: Int = 0) {
    inline val zlib: Boolean
      get() = (value and 1) != 0

    inline val gzip: Boolean
      get() = (value and 2) != 0

    inline val validate: Boolean
      get() = (value and 4) != 0

    override fun toString(): String {
      return "Wrap(zlib=$zlib, gzip=$gzip, validate=$validate)"
    }
  }

  /** True if dictionary provided */
  internal var havedict: Boolean = false

  /** Gzip header method and flags (0 if zlib), or -1 if raw or no header yet */
  internal var flags: Flags = Flags.raw

  @JvmInline
  internal value class Flags(val packed: UShort) {
    companion object Companion {
      val zlib = Flags(0.toUShort())
      val raw = Flags((-1).toUShort())
    }

    val method: Method?
      inline get() = Method.fromValue(packed.toUByte())

    val ftext: Boolean
      inline get() = (packed and 0x0100u) != 0u.toUShort()

    val fhcrc: Boolean
      inline get() = (packed and 0x0200u) != 0u.toUShort()

    val fextra: Boolean
      inline get() = (packed and 0x0400u) != 0u.toUShort()

    val fname: Boolean
      inline get() = (packed and 0x0800u) != 0u.toUShort()

    val fcomment: Boolean
      inline get() = (packed and 0x1000u) != 0u.toUShort()

    val freserved: Boolean
      inline get() = (packed and 0xe000u) != 0u.toUShort()

    override fun toString(): String {
      return "Flags(method=$method, ftext=$ftext, fhcrc=$fhcrc, fextra=$fextra, fname=$fname, fcomment=$fcomment, freserved=$freserved)"
    }
  }

  /** Zlib header max distance (INFLATE_STRICT) */
  internal var dmax: UInt = 32768u

  /** Protected copy of check value */
  internal var check: UInt = 0u

  /** Protected copy of output count */
  internal var total: ULong = 0u

  /** Where to save gzip header information */
  internal var head: GzipHeader? = null

  // Sliding window
  /** Log base 2 of requested window size */
  internal var wbits: UInt = 0u

  /** Window size or zero if not using window */
  internal var wsize: UInt = 0u

  /** Valid bytes in the window */
  internal var whave: UInt = 0u

  /** Window write index */
  internal var wnext: UInt = 0u

  /** Allocated sliding window, if needed */
  internal var window: UByteArray? = null

  // Bit accumulator
  /** Input bit accumulator */
  internal var hold: UInt = 0u

  /** Number of bits in "in" */
  internal var bits: UInt = 0u

  // For string and stored block copying
  /** Literal or length of data to copy */
  internal var length: UInt = 0u

  /** Distance back to copy string from */
  internal var offset: UInt = 0u

  // For table and code decoding
  /** Extra bits needed */
  internal var extra: UInt = 0u

  // Fixed and dynamic code tables
  /** Starting table for length/literal codes */
  internal var lencode: CodeArray = CodeArray(ENOUGH_LENS.toInt())

  /** Starting table for distance codes */
  internal var distcode: CodeArray = CodeArray(ENOUGH_DISTS.toInt())

  /** Index bits for lencode */
  internal var lenbits: UInt = 0u

  /** Index bits for distcode */
  internal var distbits: UInt = 0u

  // Dynamic table building
  /** Number of code length code lengths */
  internal var ncode: UInt = 0u

  /** Number of length code lengths */
  internal var nlen: UInt = 0u

  /** Number of distance code lengths */
  internal var ndist: UInt = 0u

  /** Number of code lengths in lens[] */
  internal var have: UInt = 0u

  /** Next available space in codes[] */
  internal var next: Nothing? = null

  /** Temporary storage for code lengths */
  internal var lens: UShortArray = UShortArray(320)

  /** Work area for code table building */
  internal var work: UShortArray = UShortArray(288)

  /*
   * Because we don't have pointers, we use lencode and distcode directly as buffers so we don't
   * need codes
   */

  /** Dynamic table for length/literal codes (Kotlin specific) */
  internal var lendyn: CodeArray = CodeArray(ENOUGH_LENS.toInt())

  /** Dynamic table for distance codes (Kotlin specific) */
  internal var distdyn: CodeArray = CodeArray(ENOUGH_DISTS.toInt())

  /** If false, allow invalid distance too far */
  internal var sane: Boolean = true

  /** Bits back of last unprocessed length/lit */
  internal var back: Int = -1

  /** Initial length of match */
  internal var was: UInt = 0u
}

private val InflateStream.validState: InflateState?
  get() = state?.let { if (it.strm == this) it else null }

public fun InflateStream.inflateResetKeep(): ReturnCode {
  val state = this.validState ?: return Z_STREAM_ERROR
  state.total = 0u
  this.total_in = 0u
  this.total_out = 0u
  this.msg = null
  if (state.wrap.value != 0) {
    /* to support ill-conceived Java test suite */
    this.adler = if (state.wrap.zlib) 1u else 0u
  }
  state.mode = HEAD
  state.last = false
  state.havedict = false
  state.flags = Flags.raw
  state.dmax = 32768u
  state.head = null /*Z_NULL*/
  state.hold = 0u
  state.bits = 0u
  // state.lencode = state.distcode = state.next = state.codes;
  state.lencode = CodeArray(ENOUGH_LENS.toInt())
  state.lendyn = CodeArray(ENOUGH_LENS.toInt())
  state.distcode = CodeArray(ENOUGH_DISTS.toInt())
  state.distdyn = CodeArray(ENOUGH_DISTS.toInt())
  state.sane = true
  state.back = -1
  return Z_OK
}

public fun InflateStream.inflateReset(): ReturnCode {
  val state = this.validState ?: return Z_STREAM_ERROR
  state.wsize = 0u
  state.whave = 0u
  state.wnext = 0u
  return inflateResetKeep()
}

public fun InflateStream.inflateReset2(windowBits: Int = WindowBits.DEF_WBITS): ReturnCode {
  val state = this.validState ?: return Z_STREAM_ERROR

  var wrap: Int
  var windowBits = windowBits

  if (windowBits < 0) {
    wrap = 0
    windowBits = -windowBits
  } else {
    wrap = (windowBits shr 4) + 5
    if (windowBits < 48) {
      windowBits = windowBits and 15
    }
  }

  /* set number of window bits, free window if different */
  if (windowBits != 0 && (windowBits < WindowBits.MIN_WBITS || windowBits > WindowBits.MAX_WBITS)) {
    return Z_STREAM_ERROR
  }
  if (state.window !== null && state.wbits.toInt() != windowBits) {
    state.window = null
  }

  /* update state and reset the rest of it */
  state.wrap = Wrap(wrap)
  state.wbits = windowBits.toUInt()
  return inflateReset()
}

public fun InflateStream.inflateInit2(windowBits: Int = WindowBits.DEF_WBITS): ReturnCode {
  this.msg = null
  /* in case we return an error */

  val state = InflateState()

  this.state = state
  state.strm = this
  state.window = null /*Z_NULL*/
  state.mode = HEAD
  /* to pass state test in inflateReset2() */
  val ret = inflateReset2(windowBits)
  if (ret !== Z_OK) {
    this.state = null /*Z_NULL*/
  }
  return ret
}

public fun InflateStream.inflateInit(): ReturnCode = inflateInit2()

@Suppress("NOTHING_TO_INLINE", "LocalVariableName")
public fun InflateStream.inflate(flush: Flush): ReturnCode {
  val state = this.validState ?: return Z_STREAM_ERROR
  val output = output ?: return Z_STREAM_ERROR
  val input = input ?: return Z_STREAM_ERROR
  //  if (input == null && this.avail_in != 0u) return Z_STREAM_ERROR

  var put = 0u
  var left = 0u
  var next = 0u
  var have = 0u
  var hold = 0u
  var bits = 0u

  var len: UInt
  var copy: UInt
  var here: Code
  var last: Code
  var from: UInt
  var from_source: UByteArray
  val hbuf = UByteArray(4)
  val order =
    ubyteArrayOf(
      16u,
      17u,
      18u,
      0u,
      8u,
      7u,
      9u,
      6u,
      10u,
      5u,
      11u,
      4u,
      12u,
      3u,
      13u,
      2u,
      14u,
      1u,
      15u,
    )

  if (state.mode === TYPE) state.mode = TYPEDO /* skip check */

  fun LOAD() {
    put = this.next_out
    left = this.avail_out
    next = this.next_in
    have = this.avail_in
    hold = state.hold
    bits = state.bits
  }

  fun RESTORE() {
    this.next_out = put
    this.avail_out = left
    this.next_in = next
    this.avail_in = have
    state.hold = hold
    state.bits = bits
  }

  LOAD()

  var _in = have
  var _out = left
  var ret = Z_OK

  fun NEEDBITS(n: UInt): Boolean {
    while (bits < n) {
      if (have == 0u) return true
      have--
      hold += input[next++.toInt()].toUInt() shl bits.toInt()
      bits += 8u
    }
    return false
  }

  fun CRC2(check: UInt, word: UInt) {
    hbuf[0] = word.toUByte()
    hbuf[1] = (word shr 8).toUByte()
    state.check = crc32b(check, hbuf, 2, 0)
  }

  fun CRC4(check: UInt, word: UInt) {
    hbuf[0] = word.toUByte()
    hbuf[1] = (word shr 8).toUByte()
    hbuf[2] = (word shr 16).toUByte()
    hbuf[3] = (word shr 24).toUByte()
    state.check = crc32b(check, hbuf, 4, 0)
  }

  fun INITBITS() {
    hold = 0u
    bits = 0u
  }

  fun BITS(n: Int) = hold and ((1u shl n) - 1u)

  fun DROPBITS(n: Int) {
    hold = hold shr n
    bits -= n.toUInt()
  }

  fun BYTEBITS() {
    hold = hold shr (bits and 7u).toInt()
    bits -= bits and 7u
  }

  fun PULLBYTE(): Boolean {
    if (have == 0u) return true
    have--
    hold += input[next++.toInt()].toUInt() shl bits.toInt()
    bits += 8u
    return false
  }

  loop@ while (true) {
    /* use if statements because we don't have fall through
    `break` in the original C source is `continue@loop` here.
    `goto: inf_leave` in the original C source is `break@loop` here. */

    if (state.mode == HEAD) {
      if (state.wrap.value == 0) {
        state.mode = TYPEDO
        continue@loop
      }
      if (NEEDBITS(16u)) break@loop
      if (state.wrap.gzip && hold == 0x8b1fu) {
        if (state.wbits == 0u) state.wbits = 15u
        state.check = CRC32_INIT
        CRC2(state.check, hold)
        INITBITS()
        state.mode = FLAGS
        continue@loop
      }
      state.head?.let { it.done = false }
      /* check if zlib header allowed */
      if (!state.wrap.zlib || ((BITS(8) shl 8) + (hold shr 8)) % 31u != 0u) {
        this.msg = "incorrect header check"
        state.mode = BAD
        continue@loop
      }
      if (BITS(4).toUByte() != Z_DEFLATED.value) {
        this.msg = "unknown compression method"
        state.mode = BAD
        continue@loop
      }
      DROPBITS(4)
      len = BITS(4) + 8u
      if (state.wbits == 0u) state.wbits = len
      if (len > 15u || len > state.wbits) {
        this.msg = "invalid window size"
        state.mode = BAD
        continue@loop
      }
      state.dmax = 1u shl state.wbits.toInt()
      state.flags = Flags.zlib
      /* indicate zlib header */
      state.check = ADLER32_INIT
      this.adler = state.check
      state.mode = if (hold and 0x200u != 0u) DICTID else TYPE
      INITBITS()
      continue@loop
    }

    if (state.mode == FLAGS) {
      if (NEEDBITS(16u)) break@loop
      state.flags = Flags(hold.toUShort())
      if (state.flags.method != Z_DEFLATED) {
        this.msg = "unknown compression method"
        state.mode = BAD
        continue@loop
      }
      if (state.flags.freserved) {
        this.msg = "unknown header flags set"
        state.mode = BAD
        continue@loop
      }
      state.head?.let { it.text = state.flags.ftext }
      if (state.flags.fhcrc && state.wrap.validate) CRC2(state.check, hold)
      INITBITS()
      state.mode = TIME
      /* falls through */
    }

    if (state.mode == TIME) {
      if (NEEDBITS(32u)) break@loop
      state.head?.let { it.time = Instant.fromEpochSeconds(hold.toLong()) }
      if (state.flags.fhcrc && state.wrap.validate) CRC4(state.check, hold)
      INITBITS()
      state.mode = OS
      /* falls through */
    }

    if (state.mode == OS) {
      if (NEEDBITS(16u)) break@loop
      state.head?.let {
        it.xflags = hold.toUByte().toInt()
        it.os = (hold shr 8).toInt()
      }
      if (state.flags.fhcrc && state.wrap.validate) CRC2(state.check, hold)
      INITBITS()
      state.mode = EXLEN
      /* falls through */
    }

    if (state.mode == EXLEN) {
      if (state.flags.fextra) {
        if (NEEDBITS(16u)) break@loop
        state.length = hold
        state.head?.let { it.extra_len = hold }
        if (state.flags.fhcrc && state.wrap.validate) CRC2(state.check, hold)
        INITBITS()
      } else state.head?.let { it.extra = null }
      state.mode = EXTRA
      /* falls through */
    }

    if (state.mode == EXTRA) {
      if (state.flags.fextra) {
        copy = state.length
        if (copy > have) copy = have
        if (copy != 0u) {
          state.head?.let { head ->
            len = head.extra_len - state.length
            if (head.extra == null) head.extra = UByteArray(size = head.extra_len.toInt())
            zmemcpy(
              dest = head.extra!!,
              destStart = len,
              src = input,
              srcStart = next,
              len = if (len + copy > head.extra_max) head.extra_max - len else copy,
            )
          }
          if (state.flags.fhcrc && state.wrap.validate)
            state.check = crc32b(state.check, input, copy.toInt(), next.toInt())
          have -= copy
          next += copy
          state.length -= copy
        }
        if (state.length != 0u) break@loop
      }
      state.length = 0u
      state.mode = NAME
      /* falls through */
    }

    if (state.mode == NAME) {
      if (state.flags.fname) {
        if (have == 0u) break@loop

        copy = 0u
        state.head?.name = ByteStringBuilder()
        do {
          len = input[(next + copy++).toInt()].toUInt()
          state.head?.let { head ->
            /* use constant limit because we do not preallocate memory */
            if (len != 0u && state.length < UShort.MAX_VALUE /*state.head.name_max*/)
              state.head?.name!!.append(len.toUByte())
          }
        } while (len != 0u && copy < have)

        if (state.flags.fhcrc && state.wrap.validate)
          state.check = crc32b(state.check, input, copy.toInt(), next.toInt())
        have -= copy
        next += copy
        if (len != 0u) break@loop
      } else state.head?.name = null

      state.length = 0u
      state.mode = COMMENT
      /* falls through */
    }

    if (state.mode == COMMENT) {
      if (state.flags.fcomment) {
        if (have == 0u) break@loop

        copy = 0u
        state.head?.name = ByteStringBuilder()
        do {
          len = input[(next + copy++).toInt()].toUInt()
          state.head?.let { head ->
            /* use constant limit because we do not preallocate memory */
            if (len != 0u && state.length < UShort.MAX_VALUE /*state.head.comm_max*/)
              state.head?.name!!.append(len.toUByte())
          }
        } while (len != 0u && copy < have)

        if (state.flags.fhcrc && state.wrap.validate)
          state.check = crc32b(state.check, input, copy.toInt(), next.toInt())

        have -= copy
        next += copy
        if (len != 0u) {
          break@loop
        }
      } else state.head?.comment = null
      state.mode = HCRC
      /* falls through */
    }

    if (state.mode == HCRC) {
      if (state.flags.fhcrc) {
        if (NEEDBITS(16u)) break@loop
        if (state.wrap.validate && hold != (state.check and 0xffffu)) {
          this.msg = "header crc mismatch"
          state.mode = BAD
          continue@loop
        }
        INITBITS()
      }
      state.head?.let {
        it.hcrc = state.flags.fhcrc
        it.done = true
      }
      this.adler = 0u
      state.check = 0u
      state.mode = TYPE
      continue@loop
    }

    if (state.mode == DICTID) {
      if (NEEDBITS(32u)) break@loop
      this.adler = zswap32(hold)
      state.check = this.adler
      INITBITS()
      state.mode = DICT
      /* falls through */
    }

    if (state.mode == DICT) {
      if (!state.havedict) {
        RESTORE()
        return Z_NEED_DICT
      }
      this.adler = ADLER32_INIT
      state.check = this.adler
      state.mode = TYPE
      /* falls through */
    }

    if (state.mode == TYPE) {
      if (flush === Z_BLOCK || flush === Z_TREES) break@loop
      state.mode = TYPEDO
      /* falls through */
    }

    if (state.mode == TYPEDO) {
      if (state.last) {
        BYTEBITS()
        state.mode = CHECK
        continue@loop
      }
      if (NEEDBITS(3u)) break@loop
      state.last = BITS(1) != 0u
      DROPBITS(1)

      when (BITS(2)) {
        0u -> state.mode = STORED
        1u -> {
          fixedtables(state)
          state.mode = LEN_
          if (flush == Z_TREES) {
            DROPBITS(2)
            break@loop
          }
        }
        2u -> state.mode = TABLE
        3u -> {
          this.msg = "invalid block type"
          state.mode = BAD
        }
      }
      DROPBITS(2)
      continue@loop
    }

    if (state.mode == STORED) {
      BYTEBITS()
      if (NEEDBITS(32u)) break@loop
      if ((hold and 0xffffu) != ((hold shr 16) xor 0xffffu)) {
        this.msg = "invalid stored block lengths"
        state.mode = BAD
        continue@loop
      }
      state.length = (hold and 0xffffu)
      INITBITS()
      state.mode = COPY_
      if (flush === Z_TREES) break@loop
      /* falls through */
    }

    if (state.mode == COPY_) {
      state.mode = COPY
      /* falls through */
    }

    if (state.mode == COPY) {
      copy = state.length
      if (copy != 0u) {
        if (copy > have) copy = have
        if (copy > left) copy = left
        if (copy == 0u) break@loop
        zmemcpy(output, put, input, next, copy)
        have -= copy
        next += copy
        left -= copy
        put += copy
        state.length -= copy
        continue@loop
      }
      state.mode = TYPE
      continue@loop
    }

    if (state.mode == TABLE) {
      if (NEEDBITS(14u)) break@loop
      state.nlen = (BITS(5) + 257u)
      DROPBITS(5)
      state.ndist = (BITS(5) + 1u)
      DROPBITS(5)
      state.ncode = (BITS(4) + 4u)
      DROPBITS(4)
      if (state.nlen > 286u || state.ndist > 30u) {
        this.msg = "too many length or distance symbols"
        state.mode = BAD
        continue@loop
      }
      state.have = 0u
      state.mode = LENLENS
      /* falls through */
    }

    if (state.mode == LENLENS) {
      while (state.have < state.ncode) {
        if (NEEDBITS(3u)) break@loop
        state.lens[order[state.have++.toInt()].toInt()] = BITS(3).toUShort()
        DROPBITS(3)
      }
      while (state.have < 19u) {
        state.lens[order[state.have++.toInt()].toInt()] = 0u
      }
      // Switch to use dynamic table
      state.lencode = state.lendyn
      state.lenbits = 7u

      val ret =
        inflate_table(
          type = CODES,
          lens = state.lens,
          lens_index = 0u,
          codes = 19u,
          table = state.lencode,
          table_index = 0u,
          work = state.work,
          bits = state.lenbits,
        ) {
          state.lenbits = it
        }

      if (ret != 0) {
        this.msg = "invalid code lengths set"
        state.mode = BAD
        continue@loop
      }
      state.have = 0u
      state.mode = CODELENS
      /* falls through */
    }

    if (state.mode == CODELENS) {
      while (state.have < state.nlen + state.ndist) {
        while (true) {
          here = state.lencode[BITS(state.lenbits.toInt()).toInt()]

          if ((here.bits) <= bits) break
          if (PULLBYTE()) break@loop
        }
        if (here.value < 16u) {
          DROPBITS(here.bits.toInt())
          state.lens[state.have++.toInt()] = here.value
        } else {
          if (here.value.toUInt() == 16u) {
            if (NEEDBITS(here.bits + 2u)) break@loop
            DROPBITS(here.bits.toInt())
            if (state.have == 0u) {
              this.msg = "invalid bit length repeat"
              state.mode = BAD
              break
            }
            len = state.lens[state.have.toInt() - 1].toUInt()
            copy = (3u + BITS(2))
            DROPBITS(2)
          } else if (here.value.toUInt() == 17u) {
            if (NEEDBITS(here.bits + 3u)) break@loop
            DROPBITS(here.bits.toInt())
            len = 0u
            copy = (3u + BITS(3))
            DROPBITS(3)
          } else {
            if (NEEDBITS(here.bits + 7u)) break@loop
            DROPBITS(here.bits.toInt())
            len = 0u
            copy = (11u + BITS(7))
            DROPBITS(7)
          }
          if (state.have + copy > state.nlen + state.ndist) {
            this.msg = "invalid bit length repeat"
            state.mode = BAD
            break
          }
          while (copy-- != 0u) state.lens[state.have++.toInt()] = len.toUShort()
        }
      }

      /* handle error breaks in while */
      if (state.mode === BAD) continue@loop

      /* check for end-of-block code (better have one) */
      if (state.lens[256].toUInt() == 0u) {
        this.msg = "invalid code -- missing end-of-block"
        state.mode = BAD
        continue@loop
      }

      /* build code tables -- note: do not change the lenbits or distbits
      values here (9 and 6) without reading the comments in inftrees.h
      concerning the ENOUGH constants, which depend on those values */
      state.lenbits = 9u
      val ret1 =
        inflate_table(
          type = LENS,
          lens = state.lens,
          lens_index = 0u,
          codes = state.nlen,
          table = state.lencode,
          table_index = 0u,
          work = state.work,
          bits = state.lenbits,
        ) {
          state.lenbits = it
        }
      if (ret1 != 0) {
        this.msg = "invalid literal/lengths set"
        state.mode = BAD
        continue@loop
      }

      state.distbits = 6u
      state.distcode = state.distdyn
      val ret2 =
        inflate_table(
          type = DISTS,
          lens = state.lens,
          lens_index = state.nlen,
          codes = state.ndist,
          table = state.distcode,
          table_index = 0u,
          work = state.work,
          bits = state.distbits,
        ) {
          state.distbits = it
        }
      if (ret2 != 0) {
        this.msg = "invalid distances set"
        state.mode = BAD
        continue@loop
      }

      state.mode = LEN_
      if (flush === Z_TREES) break@loop
      /* falls through */
    }

    if (state.mode == LEN_) {
      state.mode = LEN
      /* falls through */
    }

    if (state.mode == LEN) {
      if (have >= 6u && left >= 258u) {
        RESTORE()
        inflate_fast(_out)
        LOAD()
        if (state.mode == TYPE) state.back = -1
        continue@loop
      }
      state.back = 0
      while (true) {
        here = state.lencode[BITS(state.lenbits.toInt()).toInt()]
        if (here.bits <= bits) break
        if (PULLBYTE()) break@loop
      }
      if (here.op.toUInt() != 0u && (here.op.toUInt() and 0xf0u) == 0u) {
        last = here
        while (true) {
          here =
            state.lencode[
                (last.value + (BITS((last.bits + last.op).toInt()) shr last.bits.toInt())).toInt()]
          if ((last.bits + here.bits) <= bits) break
          if (PULLBYTE()) break@loop
        }
        DROPBITS(last.bits.toInt())
        state.back += last.bits.toInt()
      }
      DROPBITS(here.bits.toInt())
      state.back += here.bits.toInt()
      state.length = here.value.toUInt()
      if (here.op.toInt() == 0) {
        state.mode = LIT
        continue@loop
      }
      if (here.op.toUInt() and 32u != 0u) {
        state.back = -1
        state.mode = TYPE
        continue@loop
      }
      if (here.op.toUInt() and 64u != 0u) {
        this.msg = "invalid literal/length code"
        state.mode = BAD
        continue@loop
      }
      state.extra = here.op.toUInt() and 15u
      state.mode = LENEXT
      /* fallthrough */
    }

    if (state.mode == LENEXT) {
      if (state.extra != 0u) {
        if (NEEDBITS(state.extra)) break@loop
        BITS(state.extra.toInt())
        DROPBITS(state.extra.toInt())
        state.back += state.extra.toInt()
      }
      state.was = state.length
      state.mode = DIST
      /* falls through */
    }

    if (state.mode == DIST) {
      while (true) {
        here = Code(BITS(state.distbits.toInt()))
        if ((here.bits) <= bits) break
        if (PULLBYTE()) break@loop
      }
      if ((here.op and 0xf0u.toUByte()) == 0u.toUByte()) {
        last = here
        while (true) {
          here =
            state.distcode[
                (last.value.toULong() + (BITS((last.bits + last.op).toInt()) shr last.bits.toInt()))
                  .toInt()]
          if ((last.bits + here.bits) <= bits) break

          if (PULLBYTE()) break@loop
        }
        DROPBITS(last.bits.toInt())
        state.back += last.bits.toInt()
      }
      DROPBITS(here.bits.toInt())
      state.back += here.bits.toInt()
      if (here.op and 64u.toUByte() != 0u.toUByte()) {
        this.msg = "invalid distance code"
        state.mode = BAD
        continue@loop
      }
      state.offset = here.value.toUInt()
      state.extra = here.op.toUInt() and 15u
      state.mode = DISTEXT
      /* falls through */
    }

    if (state.mode == DISTEXT) {
      if (state.extra != 0u) {
        if (NEEDBITS(state.extra)) break@loop
        BITS(state.extra.toInt())
        DROPBITS(state.extra.toInt())
        state.back += state.extra.toInt()
      }
      if (state.offset > state.dmax) {
        this.msg = "invalid distance too far back"
        state.mode = BAD
        continue@loop
      }
      state.mode = MATCH
      /* falls through */
    }

    if (state.mode == MATCH) {
      if (left == 0u) break@loop
      copy = _out - left
      if (state.offset > copy) {
        /* copy from window */
        copy = state.offset - copy
        if (copy > state.whave && state.sane) {
          this.msg = "invalid distance too far back"
          state.mode = BAD
          continue@loop
        }
        if (copy > state.wnext) {
          copy -= state.wnext
          from = state.wsize - copy
        } else from = state.wnext - copy

        if (copy > state.length) copy = state.length
        from_source = state.window!!
      } else {
        /* copy from output */
        from_source = output
        from = put - state.offset
        copy = state.length
      }
      if (copy > left) {
        copy = left
      }
      left -= copy
      state.length -= copy
      do {
        output[put++.toInt()] = from_source[from++.toInt()]
        copy -= 1u
      } while (copy != 0u)
      if (state.length == 0u) state.mode = LEN
      continue@loop
    }

    if (state.mode == LIT) {
      if (left == 0u) {
        break@loop
      }
      output[put++.toInt()] = state.length.toUByte()
      left--
      state.mode = LEN
      continue@loop
    }

    if (state.mode == CHECK) {
      if (state.wrap.value != 0) {
        if (NEEDBITS(32u)) break@loop
        _out -= left
        this.total_out += _out
        state.total += _out
        if ((state.wrap.validate) && _out != 0u) {
          state.check =
            if (state.flags != Flags.zlib)
              crc32b(state.check, output, _out.toInt(), (put - _out).toInt())
            else adler32(state.check, output, _out.toInt(), (put - _out).toInt())
          this.adler = state.check
        }
        _out = left
        // NB: crc32 stored as signed 32-bit int, zswap32 returns signed too
        if (
          (state.wrap.validate) &&
            (if (state.flags != Flags.zlib) hold else zswap32(hold)) != state.check
        ) {
          this.msg = "incorrect data check"
          state.mode = BAD
          continue@loop
        }
        INITBITS()
      }
      state.mode = LENGTH
      /* falls through */
    }

    if (state.mode == LENGTH) {
      if (state.wrap.value != 0 && state.flags != Flags.zlib) {
        if (NEEDBITS(32u)) break@loop
        if ((state.wrap.validate) && hold.toULong() != (state.total and 0xffffffffu)) {
          this.msg = "incorrect length check"
          state.mode = BAD
          continue@loop
        }
        INITBITS()
      }
      state.mode = DONE
      /* falls through */
    }

    if (state.mode == DONE) {
      ret = Z_STREAM_END
      break@loop
    }

    if (state.mode == BAD) {
      ret = Z_DATA_ERROR
      break@loop
    }

    if (state.mode == MEM) return Z_MEM_ERROR
    if (state.mode == SYNC) return Z_STREAM_ERROR
    return Z_STREAM_ERROR
  }

  /*
    Return from inflate(), updating the total counts and the check value.
    If there was no progress during the inflate() call, return a buffer
    error.  Call updatewindow() to create and/or update the window state.
    Note: a memory error from inflate() is non-recoverable.
  */

  RESTORE()

  if (
    state.wsize != 0u ||
      (_out != this.avail_out && state.mode < BAD && (state.mode < CHECK || flush !== Z_FINISH))
  )
    updatewindow(output, this.next_out, _out - this.avail_out)

  _in -= this.avail_in
  _out -= this.avail_out
  this.total_in += _in
  this.total_out += _out
  state.total += _out
  if (state.wrap.validate && _out != 0u) {
    state.check =
      if (state.flags != Flags.zlib)
        crc32b(state.check, output, _out.toInt(), (next_out - _out).toInt())
      else adler32(state.check, output, _out.toInt(), (next_out - _out).toInt())
    this.adler = state.check
  }
  // strm->data_type =
  this.decode_state =
    state.bits +
      (if (state.last) 64u else 0u) +
      (if (state.mode == TYPE) 128u else 0u) +
      (if (state.mode == LEN_ || state.mode == COPY_) 256u else 0u)

  if (((_in == 0u && _out == 0u) || flush == Z_FINISH) && ret == Z_OK) ret = Z_BUF_ERROR

  return ret
}

public fun InflateStream.inflateEnd(): ReturnCode {
  val state = this.validState ?: return Z_STREAM_ERROR
  state.window = null
  this.state = null
  return Z_OK
}

public fun InflateStream.inflateGetHeader(head: GzipHeader): ReturnCode {
  /* check state */
  val state = this.validState ?: return Z_STREAM_ERROR

  if (!state.wrap.gzip) return Z_STREAM_ERROR

  /* save header structure */
  state.head = head
  head.done = false
  return Z_OK
}

/**
 * Update the window with the last wsize (normally 32K) bytes written before returning. If window
 * does not exist yet, create it. This is only called when a window is already in use, or when
 * output has been written during this inflate call, but the end of the deflate stream has not been
 * reached yet. It is also called to create a window for dictionary data when a dictionary is
 * loaded.
 *
 * Providing output buffers larger than 32K to inflate() should provide a speed advantage, since
 * only the last 32K of output is copied to the sliding window upon return from inflate(), and since
 * all distances after the first 32K of output will fall in the output data, making match copies
 * simpler and faster. The advantage may be dependent on the size of the processor's data caches.
 */
private fun InflateStream.updatewindow(src: UByteArray, end: UInt, copy: UInt) {
  val state = this.state!!

  /* if it hasn't been done already, allocate space for the window */
  val window: UByteArray =
    state.window
      ?: run {
        state.wsize = 1u shl state.wbits.toInt()
        state.wnext = 0u
        state.whave = 0u
        UByteArray(state.wsize.toInt()).also { state.window = it }
      }

  /* copy state->wsize or less output bytes into the circular window */
  if (copy >= state.wsize) {
    zmemcpy(window, 0u, src, end - state.wsize, state.wsize)
    state.wnext = 0u
    state.whave = state.wsize
  } else {
    var dist = state.wsize - state.wnext
    if (dist > copy) dist = copy
    zmemcpy(window, state.wnext, src, end - copy, dist)
    val copy = copy - dist
    if (copy != 0u) {
      zmemcpy(window, 0u, src, end - copy, copy)
      state.wnext = copy
      state.whave = state.wsize
    } else {
      state.wnext += dist
      if (state.wnext == state.wsize) state.wnext = 0u
      if (state.whave < state.wsize) state.whave += dist
    }
  }
}

public fun InflateStream.inflateSetDictionary(
  dictionary: UByteArray,
  dictLength: UInt,
): ReturnCode {
  /* check state */
  val state = this.validState ?: return Z_STREAM_ERROR

  if (state.wrap.value != 0 && state.mode !== DICT) return Z_STREAM_ERROR

  /* check for correct dictionary identifier */
  if (state.mode === DICT) {
    var dictid = ADLER32_INIT
    dictid = adler32(dictid, dictionary, dictLength.toInt(), 0u.toInt())
    if (dictid != state.check) {
      return Z_DATA_ERROR
    }
  }
  /* copy dictionary to window using updatewindow(), which will amend the
  existing dictionary if appropriate */
  updatewindow(dictionary, dictLength, dictLength)

  state.havedict = true
  return Z_OK
}

/* Not implemented:
inflateCodesUsed
inflateCopy
inflateGetDictionary
inflateMark
inflatePrime
inflateSync
inflateSyncPoint
inflateUndermine
inflateValidate
*/
