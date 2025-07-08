@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.MemLevel
import dev.sargunv.kompress.zlib.Method
import dev.sargunv.kompress.zlib.Strategy
import kotlinx.cinterop.ptr
import platform.zlib.deflate
import platform.zlib.deflateInit2
import platform.zlib.deflateReset

public actual class PlatformDeflater actual constructor(level: Int, format: Format) :
  Compressor by NativeZStreamProcessor(
    init = { stream ->
      println("deflateInit2")
      deflateInit2(
        strm = stream.ptr,
        level = level,
        method = Method.Z_DEFLATED.value.toInt(),
        windowBits = format.windowBits,
        memLevel = MemLevel.DEFAULT_MEMLEVEL,
        strategy = Strategy.Z_DEFAULT_STRATEGY.value.toInt(),
      )
    },
    process = { stream, flush ->
      println("deflate with $flush")
      deflate(stream.ptr, flush.toInt())
    },
    reset = { stream ->
      println("deflateReset")
      deflateReset(stream.ptr)
    },
  )
