package dev.sargunv.kompress.ktxio

import dev.sargunv.kompress.StreamingProcessor
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.Sink

internal class StreamingProcessorSink(
  private val sink: Sink,
  private val processor: StreamingProcessor,
) : RawSink {

  val buffer = ByteArray(8192)

  override fun write(source: Buffer, byteCount: Long) {
    require(byteCount >= 0) { "Cannot process zero or negative byte count: $byteCount" }
    var bytesRead = 0
    while (bytesRead < byteCount && !source.exhausted()) {
      val maxToRead = minOf((byteCount - bytesRead).toInt(), buffer.size)
      val endIndex = source.readAtMostTo(buffer, 0, maxToRead)
      require(endIndex >= 0) { "Unexpected EOF" }
      bytesRead += endIndex
      processor.push(
        input = buffer,
        startIndex = 0,
        endIndex = endIndex,
        finish = source.exhausted(),
      ) { output, startIndex, endIndex ->
        sink.write(output, startIndex, endIndex)
      }
    }
  }

  override fun flush() {
    sink.flush()
  }

  override fun close() {
    processor.close()
    sink.close()
  }
}
