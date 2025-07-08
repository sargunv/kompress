package dev.sargunv.kompress

/** A streaming interface that processes data in chunks. See [Compressor] and [Decompressor]. */
public sealed interface StreamingProcessor : AutoCloseable {

  /**
   * Pushes the specified chunk of data to the processor.
   *
   * @param input the data to process.
   * @param startIndex the start index of the [input] data to process, inclusive.
   * @param endIndex the end index of the [input] data to process, exclusive.
   * @param finish `true` if this is the last chunk of data.
   * @param onOutput a callback that will be invoked one or more times with the processed data.
   */
  public fun push(
    input: UByteArray,
    startIndex: Int = 0,
    endIndex: Int = input.size,
    finish: Boolean,
    onOutput: (output: UByteArray, startIndex: Int, endIndex: Int) -> Unit,
  ): Boolean

  public fun push(
    input: ByteArray,
    startIndex: Int = 0,
    endIndex: Int = input.size,
    finish: Boolean,
    onOutput: (output: ByteArray, startIndex: Int, endIndex: Int) -> Unit,
  ): Boolean =
    push(input.asUByteArray(), startIndex, endIndex, finish) { output, start, end ->
      onOutput(output.asByteArray(), start, end)
    }
}
