@file:Suppress("FunctionName", "LocalVariableName")

package dev.sargunv.kompress.zlib

import dev.sargunv.kompress.zlib.InflateMode.*

internal fun InflateStream.inflate_fast(start: UInt) {
  /* copy state to local variables */
  val state = this.state!!
  var _in = this.next_in // local strm.input
  val input = this.input!!
  val last = _in + (this.avail_in - 5u) // have enough input while in < last
  var _out = this.next_out // local strm.output
  val output = this.output!!
  val beg = _out - (start - this.avail_out) // inflate()'s initial strm.output
  val end = _out + (this.avail_out - 257u) // while out < end, enough space available
  val dmax = state.dmax // maximum distance from zlib header
  val wsize = state.wsize // window size or zero if not using window
  val whave = state.whave // valid bytes in the window
  val wnext = state.wnext // window write index
  val window = state.window // allocated sliding window, if wsize != 0
  var hold = state.hold // local strm.hold
  var bits = state.bits // local strm.bits
  val lcode = state.lencode // local strm.lencode
  val dcode = state.distcode // local strm.distcode
  val lmask = (1u shl state.lenbits.toInt()) - 1u //  mask for first level of length codes
  val dmask = (1u shl state.distbits.toInt()) - 1u // mask for first level of distance codes

  var here: Code // retrieved table entry
  var op: UInt // code bits, operation, extra bits, or window position, window bytes to copy
  var len: UInt // match length, unused bytes
  var dist: UInt // match distance
  var from: UInt // where to copy match from */
  var from_source: UByteArray?

  top@ do {
    if (bits < 15u) {
      hold += (input[_in++.toInt()].toUInt() shl bits.toInt())
      bits += 8u
      hold += input[_in++.toInt()].toUInt() shl bits.toInt()
      bits += 8u
    }

    here = lcode[(hold and lmask).toInt()]

    dolen@ while (true) { // goto emulation
      op = here.bits.toUInt()
      hold = hold shr op.toInt()
      bits -= op
      op = here.op.toUInt()

      if (op == 0u) { // literal
        output[_out++.toInt()] = here.value.toUByte()
      } else if (op and 16u != 0u) { // length base
        len = here.value.toUInt()
        op = op and 15u
        if (op != 0u) {
          if (bits < op) {
            hold += input[_in++.toInt()].toUInt() shl bits.toInt()
            bits += 8u
          }
          len += hold and ((1u shl op.toInt()) - 1u)
          hold = hold shr op.toInt()
          bits -= op
        }
        if (bits < 15u) {
          hold += input[_in++.toInt()].toUInt() shl bits.toInt()
          bits += 8u
          hold += input[_in++.toInt()].toUInt() shl bits.toInt()
          bits += 8u
        }
        here = dcode[(hold and dmask).toInt()]

        dodist@ while (true) { // goto emulation
          op = here.bits.toUInt()
          hold = hold shr op.toInt()
          bits -= op
          op = here.op.toUInt()

          if (op and 16u != 0u) { // distance base
            dist = here.value.toUInt()
            op = op and 15u /* number of extra bits */
            if (bits < op) {
              hold += input[_in++.toInt()].toUInt() shl bits.toInt()
              bits += 8u
              if (bits < op) {
                hold += input[_in++.toInt()].toUInt() shl bits.toInt()
                bits += 8u
              }
            }
            dist += hold and ((1u shl op.toInt()) - 1u)

            if (dist > dmax) {
              this.msg = "invalid distance too far back"
              state.mode = BAD
              break@top
            }

            hold = hold shr op.toInt()
            bits -= op
            op = _out - beg /* max distance in output */
            if (dist > op) {
              /* see if copy from window */
              op = dist - op /* distance back in window */
              if (op > whave) {
                if (state.sane) {
                  this.msg = "invalid distance too far back"
                  state.mode = BAD
                  break@top
                }
              }
              from = 0u // window index
              from_source = window
              if (wnext == 0u) {
                /* very common case */
                from += wsize - op
                if (op < len) {
                  /* some from window */
                  len -= op
                  do {
                    output[_out++.toInt()] = window!![from++.toInt()]
                    op -= 1u
                  } while (op != 0u)
                  from = _out - dist /* rest from output */
                  from_source = output
                }
              } else if (wnext < op) {
                /* wrap around window */
                from += wsize + wnext - op
                op -= wnext
                if (op < len) {
                  /* some from end of window */
                  len -= op
                  do {
                    output[_out++.toInt()] = window!![from++.toInt()]
                    op -= 1u
                  } while (op != 0u)
                  from = 0u
                  if (wnext < len) {
                    /* some from start of window */
                    op = wnext
                    len -= op
                    do {
                      output[_out++.toInt()] = window[from++.toInt()]
                      op -= 1u
                    } while (op != 0u)
                    from = _out - dist /* rest from output */
                    from_source = output
                  }
                }
              } else {
                /* contiguous in window */
                from += wnext - op
                if (op < len) {
                  /* some from window */
                  len -= op
                  do {
                    output[_out++.toInt()] = window!![from++.toInt()]
                    op -= 1u
                  } while (op != 0u)
                  from = _out - dist /* rest from output */
                  from_source = output
                }
              }
              while (len > 2u) {
                output[_out++.toInt()] = from_source!![from++.toInt()]
                output[_out++.toInt()] = from_source[from++.toInt()]
                output[_out++.toInt()] = from_source[from++.toInt()]
                len -= 3u
              }
              if (len != 0u) {
                output[_out++.toInt()] = from_source!![from++.toInt()]
                if (len > 1u) {
                  @Suppress("AssignedValueIsNeverRead")
                  output[_out++.toInt()] = from_source[from++.toInt()]
                }
              }
            } else {
              from = _out - dist /* copy direct from output */
              do {
                /* minimum length is three */
                output[_out++.toInt()] = output[from++.toInt()]
                output[_out++.toInt()] = output[from++.toInt()]
                output[_out++.toInt()] = output[from++.toInt()]
                len -= 3u
              } while (len > 2u)
              if (len != 0u) {
                output[_out++.toInt()] = output[from++.toInt()]
                if (len > 1u) {
                  @Suppress("AssignedValueIsNeverRead")
                  output[_out++.toInt()] = output[from++.toInt()]
                }
              }
            }
          } else if (op and 64u == 0u) { // 2nd level distance code
            here = dcode[here.value.toInt() + (hold and ((1u shl op.toInt()) - 1u)).toInt()]
            continue@dodist
          } else {
            this.msg = "invalid distance code"
            state.mode = BAD
            break@top
          }

          break
        }
      } else if (op and 64u == 0u) { // 2nd level length code
        here = lcode[here.value.toInt() + (hold and ((1u shl op.toInt()) - 1u)).toInt()]
        continue@dolen
      } else if (op and 32u != 0u) { // end-of-block
        state.mode = TYPE
        break@top
      } else {
        this.msg = "invalid literal/length code"
        state.mode = BAD
        break@top
      }

      break
    }
  } while (_in < last && _out < end)

  /* return unused bytes (on entry, bits < 8, so in won't go too far back) */
  len = bits shr 3
  _in -= len
  bits -= len shl 3
  hold = hold and ((1u shl bits.toInt()) - 1u)

  /* update state and return */
  this.next_in = _in
  this.next_out = _out
  this.avail_in = if (_in < last) (5u + (last - _in)) else (5u - (_in - last))
  this.avail_out = if (_out < end) (257u + (end - _out)) else (257u - (_out - end))
  state.hold = hold
  state.bits = bits
}
