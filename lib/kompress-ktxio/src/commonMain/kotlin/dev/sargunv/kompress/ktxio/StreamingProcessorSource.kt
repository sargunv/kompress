package dev.sargunv.kompress.ktxio

import dev.sargunv.kompress.StreamingProcessor
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source

internal class StreamingProcessorSource(
  private val source: Source,
  private val processor: StreamingProcessor,
) : RawSource {

  private val buffer = ByteArray(8192)

  override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
    if (source.exhausted()) return -1L
    var bytesRead = 0L
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
    return bytesRead
  }

  override fun close() {
    source.close()
    processor.close()
  }
}
