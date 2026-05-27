/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services.ping

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Sends a single ICMP echo (v4 or v6) and returns the round-trip time.
 *
 * The actual ICMP socket calls block, so [run] is suspending and dispatches to
 * [Dispatchers.IO] internally — callers don't need to wrap it.
 */
object Ping {
    private val ECHO_PAYLOAD = "abcdefghijklmnopqrstuvwabcdefghi".toByteArray()
    private const val IP_TOS_LOW_DELAY = 0x10
    private const val ECHO_PORT = 7
    private const val MSG_DONT_WAIT = 0x40
    private val POLL_IN = (if (OsConstants.POLLIN == 0) 1 else OsConstants.POLLIN).toShort()

    /**
     * Pings [dest] and returns the round-trip time in milliseconds.
     * Throws if anything fails (socket, send, poll, recv).
     */
    @Throws(Exception::class)
    suspend fun run(
        dest: InetAddress,
        timeoutMs: Int,
    ): Long =
        withContext(Dispatchers.IO) {
            val isV6 = dest is Inet6Address
            val type = if (isV6) EchoPacketBuilder.TYPE_ICMP_V6 else EchoPacketBuilder.TYPE_ICMP_V4
            val inet = if (isV6) OsConstants.AF_INET6 else OsConstants.AF_INET
            val proto = if (isV6) OsConstants.IPPROTO_ICMPV6 else OsConstants.IPPROTO_ICMP

            // Each ping needs a fresh build — EchoPacketBuilder embeds a unique
            // identifier per call, so the buffer can't be reused.
            val byteBuffer = EchoPacketBuilder(type, ECHO_PAYLOAD).build()
            val buffer = ByteArray(byteBuffer.limit())

            val fd = Os.socket(inet, OsConstants.SOCK_DGRAM, proto)
            if (!fd.valid()) {
                Os.close(fd)
                throw Exception("Failed to open ICMP socket")
            }

            try {
                setLowDelay(fd)
                val pollFd =
                    StructPollfd().apply {
                        this.fd = fd
                        events = POLL_IN
                    }
                val pollFds = arrayOf(pollFd)

                val start = System.currentTimeMillis()
                if (Os.sendto(fd, byteBuffer, 0, dest, ECHO_PORT) < 0) {
                    throw Exception("sendto failed")
                }
                if (Os.poll(pollFds, timeoutMs) < 0) {
                    throw Exception("poll failed")
                }
                val time = System.currentTimeMillis() - start
                if (pollFd.revents != POLL_IN) {
                    throw Exception("poll returned no data (revents=${pollFd.revents})")
                }
                if (Os.recvfrom(fd, buffer, 0, buffer.size, MSG_DONT_WAIT, null) < 0) {
                    throw Exception("recvfrom failed")
                }
                time
            } finally {
                Os.close(fd)
            }
        }

    private fun setLowDelay(fd: FileDescriptor) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            Os.setsockoptInt(fd, OsConstants.IPPROTO_IP, OsConstants.IP_TOS, IP_TOS_LOW_DELAY)
        } else {
            // Reflectively call setsockoptInt on older API levels where it isn't public.
            try {
                val method =
                    Os::class.java.getMethod(
                        "setsockoptInt",
                        FileDescriptor::class.java,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                    )
                method.invoke(null, fd, OsConstants.IPPROTO_IP, OsConstants.IP_TOS, IP_TOS_LOW_DELAY)
            } catch (_: Exception) {
            }
        }
    }
}
