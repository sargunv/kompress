package dev.sargunv.kompress.zlib

import dev.sargunv.kompress.zlib.ZStream.State

public typealias DeflateStream = ZStream<DeflateState>

public class DeflateState : State

public fun DeflateStream.deflateInit(level: Int = Level.DEFAULT_COMPRESSION): ReturnCode = TODO()

public fun DeflateStream.deflateInit2(
  level: Int = Level.DEFAULT_COMPRESSION,
  method: Method = Method.Z_DEFLATED,
  windowBits: Int = WindowBits.DEF_WBITS,
  memLevel: Int = MemLevel.DEFAULT_MEMLEVEL,
  strategy: Strategy = Strategy.Z_DEFAULT_STRATEGY,
): ReturnCode = TODO()

public fun DeflateStream.deflateReset(): ReturnCode = TODO()

public fun DeflateStream.deflateResetKeep(): ReturnCode = TODO()

public fun DeflateStream.deflateSetHeader(header: GzipHeader): ReturnCode = TODO()

public fun DeflateStream.deflate(flush: Flush): ReturnCode = TODO()

public fun DeflateStream.deflateEnd(): ReturnCode = TODO()

public fun DeflateStream.deflateSetDictionary(
  dictionary: UByteArray,
  dictLength: UInt,
): ReturnCode = TODO()
