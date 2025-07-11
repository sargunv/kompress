package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.ReturnCode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.zlib.z_stream

@OptIn(ExperimentalForeignApi::class)
internal class NativeZStreamProcessor(
  init: (stream: z_stream) -> Int,
  private val process: (stream: z_stream, finish: Boolean) -> Int,
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

  @OptIn(UnsafeNumber::class)
  override fun push(
    input: UByteArray,
    startIndex: Int,
    endIndex: Int,
    finish: Boolean,
    onOutput: (output: UByteArray, startIndex: Int, endIndex: Int) -> Unit,
  ): Boolean {
    input.usePinned { pinnedInput ->
      output.usePinned { pinnedOutput ->
        stream.next_in = pinnedInput.addressOf(startIndex)
        stream.avail_in = (endIndex - startIndex).toUInt()

        var ended: Boolean
        do {
          stream.next_out = pinnedOutput.addressOf(0)
          stream.avail_out = output.size.toUInt()

          val ret = process(stream, finish).requireSuccess()
          ended =
            when (ret) {
              ReturnCode.Z_OK -> false
              ReturnCode.Z_STREAM_END -> true
              ReturnCode.Z_NEED_DICT ->
                throw ZStreamException(
                  ReturnCode.Z_NEED_DICT,
                  "need dictionary (DICTID=${stream.adler})",
                )
              else -> error("impossible")
            }

          val bytesWritten = output.size.toUInt() - stream.avail_out
          onOutput(output, 0, bytesWritten.toInt())

          val inputEmpty = stream.avail_in == 0u
          val outputFilled = stream.avail_out == 0u
        } while ((outputFilled || !inputEmpty) && !ended)
        return ended
      }
    }
  }

  override fun close() {
    reset(stream).requireSuccess()
    // TODO reset and autoclean instead of free
    nativeHeap.free(stream.rawPtr)
    isFreed = true
  }
}
