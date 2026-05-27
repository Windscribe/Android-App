package com.windscribe.vpn.services.ping

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Pinger backed by native ICMP (via [Ping]) with a TCP/443 connect-time fallback
 * for hosts where ICMP echo is blocked or fails.
 *
 * DNS resolution is dispatched to IO; ICMP itself dispatches internally.
 */
class IcmpPinger : Pinger {
    override suspend fun ping(
        host: String,
        timeoutMs: Int,
    ): Int {
        val address =
            withContext(Dispatchers.IO) {
                runCatching { Inet4Address.getByName(host) }.getOrNull()
            } ?: return -1

        // Try ICMP first.
        runCatching { return Ping.run(address, timeoutMs).toInt() }

        // Fall back to TCP connect on 443.
        return tcpConnectLatency(address.hostAddress ?: host, timeoutMs)
    }

    private suspend fun tcpConnectLatency(
        ip: String,
        timeoutMs: Int,
    ): Int =
        withContext(Dispatchers.IO) {
            runCatching {
                val start = System.currentTimeMillis()
                Socket().use { it.connect(InetSocketAddress(ip, 443), timeoutMs) }
                (System.currentTimeMillis() - start).toInt()
            }.getOrElse { -1 }
        }
}
