@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.MemLevel
import kotlinx.cinterop.ptr
import platform.zlib.Z_DEFAULT_STRATEGY
import platform.zlib.Z_DEFLATED
import platform.zlib.Z_FINISH
import platform.zlib.Z_NO_FLUSH
import platform.zlib.deflate
import platform.zlib.deflateInit2
import platform.zlib.deflateReset

public actual class PlatformDeflater actual constructor(level: Int, format: Format) :
  Compressor by NativeZStreamProcessor(
    init = { stream ->
      deflateInit2(
        strm = stream.ptr,
        level = level,
        method = Z_DEFLATED,
        windowBits = format.windowBits,
        memLevel = MemLevel.DEFAULT_MEMLEVEL,
        strategy = Z_DEFAULT_STRATEGY,
      )
    },
    process = { stream, finish -> deflate(stream.ptr, if (finish) Z_FINISH else Z_NO_FLUSH) },
    reset = { stream -> deflateReset(stream.ptr) },
  )
