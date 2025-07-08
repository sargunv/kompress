package dev.sargunv.kompress.ktxio

import dev.sargunv.kompress.Compressor
import dev.sargunv.kompress.Decompressor
import dev.sargunv.kompress.Deflater
import dev.sargunv.kompress.Inflater
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

public class CompressorSink(
  private val sink: Sink,
  private val compressor: Compressor = Deflater(),
) : RawSink by StreamingProcessorSink(sink, compressor)

public class DecompressorSink(
  private val sink: Sink,
  private val decompressor: Decompressor = Inflater(),
) : RawSink by StreamingProcessorSink(sink, decompressor)

public class CompressorSource(
  private val source: Source,
  private val compressor: Compressor = Deflater(),
) : RawSource by StreamingProcessorSource(source, compressor)

public class DecompressorSource(
  private val source: Source,
  private val decompressor: Decompressor = Inflater(),
) : RawSource by StreamingProcessorSource(source, decompressor)

public fun Source.decompressed(inflater: Decompressor = Inflater()): DecompressorSource =
  DecompressorSource(this, inflater)

public fun Source.compressed(deflater: Compressor = Deflater()): CompressorSource =
  CompressorSource(this, deflater)

public fun Sink.decompressed(inflater: Decompressor = Inflater()): DecompressorSink =
  DecompressorSink(this, inflater)

public fun Sink.compressed(deflater: Compressor = Deflater()): CompressorSink =
  CompressorSink(this, deflater)
