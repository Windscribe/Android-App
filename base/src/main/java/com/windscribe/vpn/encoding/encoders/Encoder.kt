/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.encoding.encoders

import java.io.IOException
import java.io.OutputStream

/**
 * Encode and decode byte arrays (typically from binary to 7-bit ASCII
 * encodings).
 */
interface Encoder {
    @Throws(IOException::class)
    fun decode(
        data: String,
        out: OutputStream,
    ): Int

    @Throws(IOException::class)
    fun encode(
        data: ByteArray,
        off: Int,
        length: Int,
        out: OutputStream,
    ): Int
}
