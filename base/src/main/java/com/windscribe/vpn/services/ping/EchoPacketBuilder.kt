/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services.ping

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.internal.and

class EchoPacketBuilder(private val mType: Byte, payload: ByteArray?) {

    private val mPayload: ByteArray = if (payload == null) {
        ByteArray(0)
    } else if (payload.size > MAX_PAYLOAD) {
        throw IllegalArgumentException("Payload limited to $MAX_PAYLOAD")
    } else {
        payload
    }

    fun build(): ByteBuffer {
        val identifier = sSequence.getAndIncrement().toShort()
        val buffer = ByteArray(8 + mPayload.size)
        val byteBuffer = ByteBuffer.wrap(buffer)
        byteBuffer.put(mType)
        byteBuffer.put(CODE)
        val checkPos = byteBuffer.position()
        byteBuffer.position(checkPos + 2)
        byteBuffer.putShort(identifier)
        byteBuffer.put(mPayload)
        byteBuffer.putShort(checkPos, checksum(buffer, buffer.size))
        byteBuffer.flip()
        return byteBuffer
    }

    companion object {

        const val MAX_PAYLOAD = 65507
        const val TYPE_ICMP_V4: Byte = 8
        const val TYPE_ICMP_V6 = 128.toByte()
        private const val CODE: Byte = 0
        private val sSequence = AtomicInteger(0)
        fun checksum(data: ByteArray, end: Int): Short {
            var sum = 0
            // High bytes (even indices)
            run {
                var i = 0
                while (i < end) {
                    sum += data[i] and 0xFF shl 8
                    sum = (sum and 0xFFFF) + (sum shr 16)
                    i += 2
                }
            }
            // Low bytes (odd indices)
            var i = 1
            while (i < end) {
                sum += data[i] and 0xFF
                sum = (sum and 0xFFFF) + (sum shr 16)
                i += 2
            }
            // Fix any one's-complement errors- sometimes it is necessary to rotate twice.
            sum = (sum and 0xFFFF) + (sum shr 16)
            return (sum xor 0xFFFF).toShort()
        }
    }
}
