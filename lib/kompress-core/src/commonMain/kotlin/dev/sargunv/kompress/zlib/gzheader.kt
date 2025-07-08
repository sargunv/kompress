package dev.sargunv.kompress.zlib

import kotlin.time.Instant
import kotlinx.io.bytestring.ByteStringBuilder

/** Represents a GZIP header structure */
@Suppress("PropertyName")
public class GzipHeader {
  /** true if compressed data believed to be text */
  public var text: Boolean = false

  /** modification time */
  public var time: Instant = Instant.fromEpochSeconds(0)

  /** extra flags (not used when writing a gzip file) */
  public var xflags: Int = 0

  /** operating system */
  public var os: Int = 0

  /** pointer to extra field or null if none */
  public var extra: UByteArray? = null

  /** extra field length (valid if extra != Z_NULL) */
  public var extra_len: UInt = 0u

  /** space at extra (only when reading header) */
  public var extra_max: UInt = 0u

  /** file name or null */
  public var name: ByteStringBuilder? = null

  /** comment or null */
  public var comment: ByteStringBuilder? = null

  /** true if there was or will be a header crc */
  public var hcrc: Boolean = false

  /** true when done reading gzip header (not used when writing a gzip file) */
  public var done: Boolean = false
}
