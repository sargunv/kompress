package dev.sargunv.kompress.zlib

@Suppress("PropertyName")
public class ZStream<T : ZStream.State> {
  /** Input buffer */
  public var input: UByteArray? = null

  /** Next input byte position */
  public var next_in: UInt = 0u

  /** Number of bytes available at input */
  public var avail_in: UInt = 0u

  /** Total number of input bytes read so far */
  public var total_in: ULong = 0u
    internal set

  /** Output buffer */
  public var output: UByteArray? = null

  /** Next output byte position */
  public var next_out: UInt = 0u

  /** Remaining free space at output */
  public var avail_out: UInt = 0u

  /** Total number of bytes output so far */
  public var total_out: ULong = 0u
    internal set

  /** Last error message, null if no error */
  public var msg: String? = null
    internal set

  /** Best guess about the data type: binary or text for [deflate] */
  public var data_type: DataType = DataType.Z_UNKNOWN
    internal set

  /** The decoding state for [inflate] (this is also [data_type] in the original zlib) */
  public var decode_state: UInt = 0u

  /** Adler-32 value of the uncompressed data */
  public var adler: UInt = 0u
    internal set

  internal var state: T? = null

  public sealed interface State
}
