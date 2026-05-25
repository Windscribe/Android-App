package com.windscribe.vpn.services.ping

/**
 * Resolves a host and measures ICMP round-trip time.
 *
 * Extracted as an interface so callers (e.g. LocationRepository) can be tested
 * without doing real network I/O.
 */
interface Pinger {
    /**
     * Pings the given host, blocking up to [timeoutMs] milliseconds.
     * Returns the round-trip time in milliseconds on success, or -1 on any failure
     * (DNS resolution failure, timeout, unreachable, native error, etc.).
     */
    suspend fun ping(host: String, timeoutMs: Int): Int
}
