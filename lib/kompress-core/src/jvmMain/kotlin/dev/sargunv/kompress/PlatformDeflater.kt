package dev.sargunv.kompress

public actual class PlatformDeflater actual constructor(level: Int, format: Format) : Compressor {

  private val output = UByteArray(8192)
  private val deflater = java.util.zip.Deflater(level, format == Format.Raw)

  actual override fun push(
    input: UByteArray,
    startIndex: Int,
    endIndex: Int,
    finish: Boolean,
    onOutput: (UByteArray, Int, Int) -> Unit,
  ): Boolean {
    deflater.setInput(input.asByteArray(), startIndex, endIndex - startIndex)
    if (finish) deflater.finish()
    do {
      val bytesWritten = deflater.deflate(output.asByteArray(), 0, output.size)
      onOutput(output, 0, bytesWritten)
    } while (bytesWritten != 0 || !deflater.needsInput())
    return deflater.finished()
  }

  actual override fun close() {
    deflater.end()
  }
}
