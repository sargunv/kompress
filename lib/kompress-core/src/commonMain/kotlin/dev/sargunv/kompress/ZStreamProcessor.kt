package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.Flush
import dev.sargunv.kompress.zlib.ReturnCode

internal class ZStreamProcessor<ZStream : dev.sargunv.kompress.zlib.ZStream<*>>(
  new: () -> ZStream,
  init: ZStream.() -> ReturnCode,
  private val process: ZStream.(Flush) -> ReturnCode,
  private val reset: ZStream.() -> ReturnCode,
  chunkSize: Int = 8192,
) : Decompressor, Compressor {
  private val output = UByteArray(chunkSize)
  private val stream: ZStream = new()

  private fun ReturnCode.requireSuccess() =
    if (value >= 0) this else throw ZStreamException(this, stream.msg)

  init {
    stream.init().requireSuccess()
  }

  override fun push(
    input: UByteArray,
    startIndex: Int,
    endIndex: Int,
    finish: Boolean,
    onOutput: (output: UByteArray, startIndex: Int, endIndex: Int) -> Unit,
  ): Boolean {
    stream.input = input
    stream.next_in = startIndex.toUInt()
    stream.avail_in = (endIndex - startIndex).toUInt()

    var ended: Boolean
    do {
      stream.output = output
      stream.next_out = 0u
      stream.avail_out = output.size.toUInt()

      ended =
        when (stream.process(if (finish) Flush.Z_FINISH else Flush.Z_NO_FLUSH).requireSuccess()) {
          ReturnCode.Z_OK -> false
          ReturnCode.Z_STREAM_END -> true
          ReturnCode.Z_NEED_DICT -> throw ZStreamException(ReturnCode.Z_NEED_DICT, null)
          else -> error("impossible")
        }

      val bytesWritten = output.size.toUInt() - stream.avail_out
      onOutput(output, 0, bytesWritten.toInt())

      val outputFilled = stream.avail_out == 0u
      val inputEmpty = stream.avail_in == 0u
    } while ((outputFilled || !inputEmpty) && !ended)
    return ended
  }

  override fun close() {
    stream.reset().requireSuccess()
  }
}
