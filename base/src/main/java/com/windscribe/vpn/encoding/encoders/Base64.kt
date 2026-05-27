/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.encoding.encoders

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

object Base64 {
    private val encoder: Encoder = Base64Encoder()

    /**
     * decode the base 64 encoded String data - whitespace will be ignored.
     *
     * @return a byte array representing the decoded data.
     */
    @JvmStatic
    fun decode(data: String): ByteArray {
        val len = data.length / 4 * 3
        val bOut = ByteArrayOutputStream(len)

        try {
            encoder.decode(data, bOut)
        } catch (e: IOException) {
            throw RuntimeException("exception decoding base64 string: $e")
        }

        return bOut.toByteArray()
    }

    /**
     * encode the input data producing a base 64 encoded byte array.
     *
     * @return a byte array containing the base 64 encoded data.
     */
    @JvmStatic
    fun encode(data: ByteArray): ByteArray {
        val len = (data.size + 2) / 3 * 4
        val bOut = ByteArrayOutputStream(len)

        try {
            encoder.encode(data, 0, data.size, bOut)
        } catch (e: IOException) {
            throw RuntimeException("exception encoding base64 string: $e")
        }

        return bOut.toByteArray()
    }

    /**
     * Encode the byte data to base 64 writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun encode(
        data: ByteArray,
        out: OutputStream,
    ): Int = encoder.encode(data, 0, data.size, out)

    /**
     * Encode the byte data to base 64 writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun encode(
        data: ByteArray,
        off: Int,
        length: Int,
        out: OutputStream,
    ): Int = encoder.encode(data, off, length, out)
}
