package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.ReturnCode

public actual class PlatformInflater actual constructor(format: Format) : Decompressor {

  private val output = UByteArray(8192)
  private val inflater = java.util.zip.Inflater(format == Format.Raw)

  actual override fun push(
    input: UByteArray,
    startIndex: Int,
    endIndex: Int,
    finish: Boolean,
    onOutput: (UByteArray, Int, Int) -> Unit,
  ): Boolean {
    inflater.setInput(input.asByteArray(), startIndex, endIndex - startIndex)
    do {
      val bytesWritten = inflater.inflate(output.asByteArray(), 0, output.size)
      onOutput(output, 0, bytesWritten)
      if (bytesWritten == 0 && inflater.needsDictionary())
        throw ZStreamException(ReturnCode.Z_NEED_DICT, "need dictionary (DICTID=${inflater.adler})")
    } while (bytesWritten != 0 || !inflater.needsInput())
    return inflater.finished()
  }

  actual override fun close() {
    inflater.end()
  }
}
