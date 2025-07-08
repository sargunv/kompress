@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.sargunv.kompress

import kotlinx.cinterop.ptr
import platform.zlib.Z_NO_FLUSH
import platform.zlib.inflate
import platform.zlib.inflateInit2
import platform.zlib.inflateReset

public actual class PlatformInflater actual constructor(format: Format) :
  Decompressor by NativeZStreamProcessor(
    init = { stream -> inflateInit2(stream.ptr, windowBits = format.windowBits) },
    process = { stream, _ -> inflate(stream.ptr, Z_NO_FLUSH) },
    reset = { stream -> inflateReset(stream.ptr) },
  )
