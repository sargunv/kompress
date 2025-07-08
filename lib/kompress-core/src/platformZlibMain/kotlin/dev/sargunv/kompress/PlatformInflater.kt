package dev.sargunv.kompress

/**
 * A stateful [Decompressor] backed by:
 * - `java.util.zip.Inflater` on JVM
 * - `platform.zlib.inflate` on Native
 */
public expect class PlatformInflater(format: Format = Format.Zlib) : Decompressor {
  override fun push(
    input: UByteArray,
    startIndex: Int,
    endIndex: Int,
    finish: Boolean,
    onOutput: (UByteArray, Int, Int) -> Unit,
  ): Boolean

  override fun close()
}
