package dev.sargunv.kompress

import dev.sargunv.kompress.zlib.ReturnCode

public class ZStreamException internal constructor(public val code: ReturnCode, message: String?) :
  Exception("${message ?: code.message} (${code})")
