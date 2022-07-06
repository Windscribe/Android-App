/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services.ping

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import io.reactivex.Single
import java.io.FileDescriptor
import java.lang.reflect.InvocationTargetException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.SocketException
import java.nio.ByteBuffer

open class Ping {

    @Throws(Exception::class)
    fun run(mDest: InetAddress?, timeoutMs: Int): Single<Long> {
        val type = if (mDest is Inet6Address) EchoPacketBuilder.TYPE_ICMP_V6 else EchoPacketBuilder.TYPE_ICMP_V4
        val mEchoPacketBuilder = EchoPacketBuilder(
                type,
                "abcdefghijklmnopqrstuvwabcdefghi".toByteArray()
        )
        val inet: Int
        val proto: Int
        if (mDest is Inet6Address) {
            inet = OsConstants.AF_INET6
            proto = OsConstants.IPPROTO_ICMPV6
        } else {
            inet = OsConstants.AF_INET
            proto = OsConstants.IPPROTO_ICMP
        }
        val fd = socket(inet, proto)
        return if (fd.valid()) {
            setLowDelay(fd)
            val structPollfd = StructPollfd()
            structPollfd.fd = fd
            structPollfd.events = Polling
            val structPollfds = arrayOf(structPollfd)
            val byteBuffer = mEchoPacketBuilder.build()
            val buffer = ByteArray(byteBuffer.limit())
            val start = System.currentTimeMillis()
            var rc = sendto(fd, byteBuffer, mDest)
            if (rc >= 0) {
                rc = poll(structPollfds, timeoutMs)
                val time = calcLatency(start, System.currentTimeMillis())
                if (rc >= 0) {
                    if (structPollfd.revents == Polling) {
                        structPollfd.revents = 0
                        rc = recvfrom(fd, buffer)
                        if (rc < 0) {
                            close(fd)
                            throw Exception()
                        }
                        Single.fromCallable {
                            close(fd)
                            time
                        }
                    } else {
                        Single.fromCallable {
                            close(fd)
                            throw Exception()
                        }
                    }
                } else {
                    close(fd)
                    throw Exception()
                }
            } else {
                close(fd)
                throw Exception()
            }
        } else {
            close(fd)
            throw Exception()
        }
    }

    /*
     * Testability methods
     */
    @Throws(ErrnoException::class)
    protected fun close(fd: FileDescriptor?) {
        Os.close(fd)
    }

    @Throws(ErrnoException::class)
    protected fun poll(structPollfds: Array<StructPollfd>?, mTimeoutMs: Int): Int {
        return Os.poll(structPollfds, mTimeoutMs)
    }

    @Throws(ErrnoException::class, SocketException::class)
    protected fun recvfrom(fd: FileDescriptor?, buffer: ByteArray): Int {
        return Os.recvfrom(fd, buffer, 0, buffer.size, msgDontWait, null)
    }

    @Throws(ErrnoException::class, SocketException::class)
    protected fun sendto(fd: FileDescriptor?, byteBuffer: ByteBuffer?, mDest: InetAddress?): Int {
        return Os.sendto(fd, byteBuffer, 0, mDest, ECHO_PORT)
    }

    @Throws(ErrnoException::class)
    protected fun socket(inet: Int, proto: Int): FileDescriptor {
        return Os.socket(inet, OsConstants.SOCK_DGRAM, proto)
    }

    private fun calcLatency(startTimestamp: Long, endTimestamp: Long): Long {
        return endTimestamp - startTimestamp
    }

    @Throws(ErrnoException::class)
    private fun setLowDelay(fd: FileDescriptor) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            Os.setsockoptInt(fd, OsConstants.IPPROTO_IP, OsConstants.IP_TOS, ipTosLowDelay)
        } else {
            try {
                val method = Os::class.java
                        .getMethod(
                                "setsockoptInt",
                                FileDescriptor::class.java,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType
                        )
                method.invoke(null, fd, OsConstants.IPPROTO_IP, OsConstants.IP_TOS, ipTosLowDelay)
            } catch (e: NoSuchMethodException) {
            } catch (e: InvocationTargetException) {
            } catch (e: IllegalAccessException) {
            }
        }
    }

    companion object {

        private const val ipTosLowDelay = 0x10
        private const val ECHO_PORT = 7
        private val Polling = (if (OsConstants.POLLIN == 0) 1 else OsConstants.POLLIN).toShort()
        private const val msgDontWait = 0x40
    }
}
