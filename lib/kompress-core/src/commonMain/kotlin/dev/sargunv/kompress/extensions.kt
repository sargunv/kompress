package dev.sargunv.kompress

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.buildByteString

public fun ByteString.decompressed(decompressor: Decompressor = Inflater()): ByteString =
  buildByteString {
    decompressor.use {
      it.push(this@decompressed.toByteArray(), finish = true, onOutput = ::append)
    }
  }

public fun ByteString.compressed(compressor: Compressor = Deflater()): ByteString =
  buildByteString {
    compressor.use { it.push(this@compressed.toByteArray(), finish = true, onOutput = ::append) }
  }

public fun ByteArray.compressed(compressor: Compressor = Deflater()): ByteArray =
  buildByteString {
      compressor.use { it.push(this@compressed, finish = true, onOutput = ::append) }
    }
    .toByteArray()

public fun ByteArray.decompressed(decompressor: Decompressor = Inflater()): ByteArray =
  buildByteString {
      decompressor.use { it.push(this@decompressed, finish = true, onOutput = ::append) }
    }
    .toByteArray()

public fun UByteArray.compressed(compressor: Compressor = Deflater()): UByteArray =
  asByteArray().compressed(compressor).asUByteArray()

public fun UByteArray.decompressed(decompressor: Decompressor = Inflater()): UByteArray =
  asByteArray().decompressed(decompressor).asUByteArray()
