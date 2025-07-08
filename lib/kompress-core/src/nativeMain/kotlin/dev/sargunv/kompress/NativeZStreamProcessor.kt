package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.Flush
import dev.sargunv.kompress.zlib.ReturnCode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.zlib.z_stream

@OptIn(ExperimentalForeignApi::class)
internal class NativeZStreamProcessor(
  init: (stream: z_stream) -> Int,
  private val process: (stream: z_stream, flush: UInt) -> Int,
  private val reset: (stream: z_stream) -> Int,
  chunkSize: Int = 8192,
) : Compressor, Decompressor {
  private val output = UByteArray(chunkSize)
  private var isFreed = false

  private val stream: z_stream =
    nativeHeap.alloc {
      zalloc = null
      zfree = null
      opaque = null
      next_in = null
      avail_in = 0u
      next_out = null
      avail_out = 0u
    }
    get() {
      if (isFreed) error("use after free")
      return field
    }

  private fun Int.requireSuccess(): ReturnCode =
    if (this >= 0) ReturnCode.fromValue(this)!!
    else throw ZStreamException(ReturnCode.fromValue(this)!!, stream.msg?.toKString())

  init {
    init(stream).requireSuccess()
  }

  override fun push(
    input: UByteArray,
    startIndex: Int,
    endIndex: Int,
    finish: Boolean,
    onOutput: (output: UByteArray, startIndex: Int, endIndex: Int) -> Unit,
  ): Boolean {
    input.usePinned { pinnedInput ->
      output.usePinned { pinnedOutput ->
        stream.next_in = pinnedOutput.addressOf(startIndex)
        stream.avail_in = (endIndex - startIndex).toUInt()
        var ended: Boolean
        do {
          stream.next_out = pinnedOutput.addressOf(0)
          stream.avail_out = output.size.toUInt()

          ended =
            when (
              process(stream, if (finish) Flush.Z_FINISH.value else Flush.Z_NO_FLUSH.value)
                .requireSuccess()
            ) {
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
    }
  }

  override fun close() {
    reset(stream).requireSuccess()
    nativeHeap.free(stream.rawPtr)
    isFreed = true
  }
}
