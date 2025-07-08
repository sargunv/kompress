package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.Level

/**
 * A stateful [Compressor] backed by:
 * - `java.util.zip.Deflater` on JVM
 * - `platform.zlib.deflate` on Native
 */
public expect class PlatformDeflater(
  level: Int = Level.DEFAULT_COMPRESSION,
  format: Format = Format.Zlib,
) : Compressor {
  override fun push(
    input: UByteArray,
    startIndex: Int,
    endIndex: Int,
    finish: Boolean,
    onOutput: (UByteArray, Int, Int) -> Unit,
  ): Boolean

  override fun close()
}
