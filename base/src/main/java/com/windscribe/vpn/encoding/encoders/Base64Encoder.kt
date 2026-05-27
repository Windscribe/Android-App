/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.encoding.encoders

import java.io.IOException
import java.io.OutputStream

class Base64Encoder : Encoder {
    /*
     * set up the decoding table.
     */
    protected val decodingTable = ByteArray(128)

    protected val encodingTable =
        byteArrayOf(
            'A'.code.toByte(),
            'B'.code.toByte(),
            'C'.code.toByte(),
            'D'.code.toByte(),
            'E'.code.toByte(),
            'F'.code.toByte(),
            'G'.code.toByte(),
            'H'.code.toByte(),
            'I'.code.toByte(),
            'J'.code.toByte(),
            'K'.code.toByte(),
            'L'.code.toByte(),
            'M'.code.toByte(),
            'N'.code.toByte(),
            'O'.code.toByte(),
            'P'.code.toByte(),
            'Q'.code.toByte(),
            'R'.code.toByte(),
            'S'.code.toByte(),
            'T'.code.toByte(),
            'U'.code.toByte(),
            'V'.code.toByte(),
            'W'.code.toByte(),
            'X'.code.toByte(),
            'Y'.code.toByte(),
            'Z'.code.toByte(),
            'a'.code.toByte(),
            'b'.code.toByte(),
            'c'.code.toByte(),
            'd'.code.toByte(),
            'e'.code.toByte(),
            'f'.code.toByte(),
            'g'.code.toByte(),
            'h'.code.toByte(),
            'i'.code.toByte(),
            'j'.code.toByte(),
            'k'.code.toByte(),
            'l'.code.toByte(),
            'm'.code.toByte(),
            'n'.code.toByte(),
            'o'.code.toByte(),
            'p'.code.toByte(),
            'q'.code.toByte(),
            'r'.code.toByte(),
            's'.code.toByte(),
            't'.code.toByte(),
            'u'.code.toByte(),
            'v'.code.toByte(),
            'w'.code.toByte(),
            'x'.code.toByte(),
            'y'.code.toByte(),
            'z'.code.toByte(),
            '0'.code.toByte(),
            '1'.code.toByte(),
            '2'.code.toByte(),
            '3'.code.toByte(),
            '4'.code.toByte(),
            '5'.code.toByte(),
            '6'.code.toByte(),
            '7'.code.toByte(),
            '8'.code.toByte(),
            '9'.code.toByte(),
            '+'.code.toByte(),
            '/'.code.toByte(),
        )

    protected val padding = '='.code.toByte()

    init {
        initialiseDecodingTable()
    }

    /**
     * decode the base 64 encoded String data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    @Throws(IOException::class)
    override fun decode(
        data: String,
        out: OutputStream,
    ): Int {
        val b1: Byte
        val b2: Byte
        val b3: Byte
        val b4: Byte
        var length = 0

        var end = data.length

        while (end > 0) {
            if (!ignore(data[end - 1])) {
                break
            }
            end--
        }

        var i = 0
        val finish = end - 4

        i = nextI(data, i, finish)

        while (i < finish) {
            val v1 = decodingTable[data[i++].code]

            i = nextI(data, i, finish)

            val v2 = decodingTable[data[i++].code]

            i = nextI(data, i, finish)

            val v3 = decodingTable[data[i++].code]

            i = nextI(data, i, finish)

            val v4 = decodingTable[data[i++].code]

            out.write((v1.toInt() shl 2) or (v2.toInt() shr 4))
            out.write((v2.toInt() shl 4) or (v3.toInt() shr 2))
            out.write((v3.toInt() shl 6) or v4.toInt())

            length += 3

            i = nextI(data, i, finish)
        }

        length +=
            decodeLastBlock(
                out,
                data[end - 4],
                data[end - 3],
                data[end - 2],
                data[end - 1],
            )

        return length
    }

    /**
     * encode the input data producing a base 64 output stream.
     *
     * @return the number of bytes produced.
     */
    @Throws(IOException::class)
    override fun encode(
        data: ByteArray,
        off: Int,
        length: Int,
        out: OutputStream,
    ): Int {
        val modulus = length % 3
        val dataLength = length - modulus
        var a1: Int
        var a2: Int
        var a3: Int

        var i = off
        while (i < off + dataLength) {
            a1 = data[i].toInt() and 0xff
            a2 = data[i + 1].toInt() and 0xff
            a3 = data[i + 2].toInt() and 0xff

            out.write(encodingTable[(a1 ushr 2) and 0x3f].toInt())
            out.write(encodingTable[((a1 shl 4) or (a2 ushr 4)) and 0x3f].toInt())
            out.write(encodingTable[((a2 shl 2) or (a3 ushr 6)) and 0x3f].toInt())
            out.write(encodingTable[a3 and 0x3f].toInt())
            i += 3
        }

        /*
         * process the tail end.
         */
        val b1: Int
        val b2: Int
        val b3: Int
        val d1: Int
        val d2: Int

        when (modulus) {
            0 -> {}

            // nothing left to do
            1 -> {
                d1 = data[off + dataLength].toInt() and 0xff
                b1 = (d1 ushr 2) and 0x3f
                b2 = (d1 shl 4) and 0x3f

                out.write(encodingTable[b1].toInt())
                out.write(encodingTable[b2].toInt())
                out.write(padding.toInt())
                out.write(padding.toInt())
            }

            2 -> {
                d1 = data[off + dataLength].toInt() and 0xff
                d2 = data[off + dataLength + 1].toInt() and 0xff

                b1 = (d1 ushr 2) and 0x3f
                b2 = ((d1 shl 4) or (d2 ushr 4)) and 0x3f
                b3 = (d2 shl 2) and 0x3f

                out.write(encodingTable[b1].toInt())
                out.write(encodingTable[b2].toInt())
                out.write(encodingTable[b3].toInt())
                out.write(padding.toInt())
            }
        }

        return (dataLength / 3) * 4 + (if (modulus == 0) 0 else 4)
    }

    protected fun initialiseDecodingTable() {
        for (i in encodingTable.indices) {
            decodingTable[encodingTable[i].toInt()] = i.toByte()
        }
    }

    @Throws(IOException::class)
    private fun decodeLastBlock(
        out: OutputStream,
        c1: Char,
        c2: Char,
        c3: Char,
        c4: Char,
    ): Int {
        val b1: Byte
        val b2: Byte
        val b3: Byte
        val b4: Byte

        return if (c3.code.toByte() == padding) {
            b1 = decodingTable[c1.code]
            b2 = decodingTable[c2.code]

            out.write((b1.toInt() shl 2) or (b2.toInt() shr 4))

            1
        } else if (c4.code.toByte() == padding) {
            b1 = decodingTable[c1.code]
            b2 = decodingTable[c2.code]
            b3 = decodingTable[c3.code]

            out.write((b1.toInt() shl 2) or (b2.toInt() shr 4))
            out.write((b2.toInt() shl 4) or (b3.toInt() shr 2))

            2
        } else {
            b1 = decodingTable[c1.code]
            b2 = decodingTable[c2.code]
            b3 = decodingTable[c3.code]
            b4 = decodingTable[c4.code]

            out.write((b1.toInt() shl 2) or (b2.toInt() shr 4))
            out.write((b2.toInt() shl 4) or (b3.toInt() shr 2))
            out.write((b3.toInt() shl 6) or b4.toInt())

            3
        }
    }

    private fun ignore(c: Char): Boolean = c == '\n' || c == '\r' || c == '\t' || c == ' '

    private fun nextI(
        data: String,
        i: Int,
        finish: Int,
    ): Int {
        var idx = i
        while (idx < finish && ignore(data[idx])) {
            idx++
        }
        return idx
    }
}
