package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.Flush
import dev.sargunv.kompress.zlib.InflateStream
import dev.sargunv.kompress.zlib.inflate
import dev.sargunv.kompress.zlib.inflateInit2
import dev.sargunv.kompress.zlib.inflateReset

/** A stateful [Decompressor] backed by a pure Kotlin port of zlib. */
public class Inflater(format: Format = Format.Zlib) :
  Decompressor by ZStreamProcessor(
    new = { InflateStream() },
    init = { inflateInit2(windowBits = format.windowBits) },
    process = { _ -> inflate(Flush.Z_NO_FLUSH) },
    reset = { inflateReset() },
  )
