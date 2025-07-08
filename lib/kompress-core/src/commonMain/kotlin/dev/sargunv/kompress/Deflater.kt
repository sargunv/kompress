package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.DeflateStream
import dev.sargunv.kompress.zlib.Flush
import dev.sargunv.kompress.zlib.Level
import dev.sargunv.kompress.zlib.deflate
import dev.sargunv.kompress.zlib.deflateInit2
import dev.sargunv.kompress.zlib.deflateReset

/** A stateful [Compressor] backed by a pure Kotlin port of zlib. */
public class Deflater(level: Int = Level.DEFAULT_COMPRESSION, format: Format = Format.Zlib) :
  Compressor by ZStreamProcessor(
    new = { DeflateStream() },
    init = { deflateInit2(level = level, windowBits = format.windowBits) },
    process = { finished -> deflate(if (finished) Flush.Z_FINISH else Flush.Z_NO_FLUSH) },
    reset = { deflateReset() },
  )
