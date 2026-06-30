package com.windscribe.vpn.backend.utils

import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.util.Log
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.ExcludedIpDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcludedIpHolder
    @Inject
    constructor(
        private val localDbInterface: LocalDbInterface,
    ) {
        private val logger = LoggerFactory.getLogger("vpn")
        private var excludedIps: List<String> = emptyList()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Loads excluded IPs from cache ONLY - does NOT attempt any DNS resolution.
         * Used during VPN connection to avoid delays. Use forceRefreshAll() or pull-to-refresh
         * to update hostname IPs.
         */
        suspend fun loadCachedIps() {
            excludedIps =
                withContext(Dispatchers.IO) {
                    try {
                        val entries = localDbInterface.getAllExcludedIpsDomains()
                        val resolvedIps = mutableListOf<String>()

                        entries.forEach { entry ->
                            when (entry.type) {
                                ExcludedIpDomain.EntryType.IP -> {
                                    resolvedIps.add(entry.value)
                                }

                                ExcludedIpDomain.EntryType.IP_RANGE -> {
                                    resolvedIps.add(entry.value)
                                }

                                ExcludedIpDomain.EntryType.HOSTNAME -> {
                                    if (!entry.resolvedIps.isNullOrEmpty()) {
                                        val cachedIps = parseResolvedIps(entry.resolvedIps)
                                        resolvedIps.addAll(cachedIps)
                                    }
                                }
                            }
                        }

                        resolvedIps
                    } catch (e: Exception) {
                        logger.error("Failed to load excluded IPs: ${e.message}")
                        emptyList()
                    }
                }
        }

        /**
         * Resolves a single newly added entry if it's a hostname, then reloads cache.
         * For IPs/ranges, just reloads the cache.
         */
        suspend fun resolveNewEntry(entry: ExcludedIpDomain) {
            withContext(Dispatchers.IO) {
                try {
                    if (entry.type == ExcludedIpDomain.EntryType.HOSTNAME) {
                        resolveAndUpdateHostname(entry)
                    }
                    loadCachedIps()
                } catch (e: Exception) {
                    logger.error("Failed to resolve new entry: ${e.message}")
                }
            }
        }

        /**
         * Resolves multiple newly added hostnames, then reloads cache once.
         * Efficient for batch imports.
         */
        suspend fun resolveNewEntries(entries: List<ExcludedIpDomain>) {
            withContext(Dispatchers.IO) {
                try {
                    entries.forEach { entry ->
                        if (entry.type == ExcludedIpDomain.EntryType.HOSTNAME) {
                            resolveAndUpdateHostname(entry)
                        }
                    }
                    loadCachedIps()
                } catch (e: Exception) {
                    logger.error("Failed to resolve new entries: ${e.message}")
                }
            }
        }

        /**
         * Forces resolution of all hostnames immediately (for pull-to-refresh).
         * This method blocks until all hostnames are resolved.
         */
        suspend fun forceRefreshAll() {
            withContext(Dispatchers.IO) {
                try {
                    val hostnames = localDbInterface.getAllExcludedHostnames()
                    hostnames.forEach { entry ->
                        resolveAndUpdateHostname(entry)
                    }
                    // Reload the cache after refresh (use loadCachedIps since we just resolved everything)
                    loadCachedIps()
                } catch (e: Exception) {
                    logger.error("Failed to force refresh hostnames: ${e.message}")
                }
            }
        }

        /**
         * Resolves a hostname and updates the database with resolved IPs.
         */
        private suspend fun resolveAndUpdateHostname(entry: ExcludedIpDomain) {
            try {
                val addresses =
                    withContext(Dispatchers.IO) {
                        InetAddress.getAllByName(entry.value)
                    }
                val ipv4Addresses = mutableListOf<String>()
                addresses.forEach { address ->
                    if (address is Inet4Address) {
                        val ip = address.hostAddress
                        if (ip != null) {
                            ipv4Addresses.add(ip)
                        }
                    }
                }

                if (ipv4Addresses.isNotEmpty()) {
                    val resolvedIpsJson = formatResolvedIps(ipv4Addresses)
                    localDbInterface.updateExcludedIpDomainResolvedData(
                        id = entry.id,
                        resolvedIps = resolvedIpsJson,
                        timestamp = System.currentTimeMillis(),
                        error = null,
                    )
                } else {
                    localDbInterface.updateExcludedIpDomainResolvedData(
                        id = entry.id,
                        resolvedIps = null,
                        timestamp = System.currentTimeMillis(),
                        error = "No IPv4 addresses found",
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to resolve hostname ${entry.value}: ${e.message}")
                localDbInterface.updateExcludedIpDomainResolvedData(
                    id = entry.id,
                    resolvedIps = entry.resolvedIps, // Keep old IPs if any
                    timestamp = System.currentTimeMillis(),
                    error = e.message,
                )
            }
        }

        fun applyExcludedRoutes(builder: VpnService.Builder) {
            applyExcludedRoutes(builder, excludedIps)
        }

        fun clear() {
            excludedIps = emptyList()
        }

        private fun parseResolvedIps(json: String): List<String> =
            try {
                val jsonArray = JSONArray(json)
                List(jsonArray.length()) { jsonArray.getString(it) }
            } catch (e: Exception) {
                logger.error("Failed to parse resolved IPs JSON: $json", e)
                emptyList()
            }

        private fun formatResolvedIps(ips: List<String>): String {
            val jsonArray = JSONArray()
            ips.forEach { jsonArray.put(it) }
            return jsonArray.toString()
        }

        companion object {
            @JvmStatic
            fun applyExcludedRoutes(
                builder: VpnService.Builder,
                routes: List<String>,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    routes.forEach { ipOrRange ->
                        try {
                            val ipPrefix =
                                if (ipOrRange.contains("/")) {
                                    val parts = ipOrRange.split("/")
                                    val address = InetAddress.getByName(parts[0])
                                    IpPrefix(address, parts[1].toInt())
                                } else {
                                    val address = InetAddress.getByName(ipOrRange)
                                    IpPrefix(address, 32)
                                }
                            builder.excludeRoute(ipPrefix)
                        } catch (e: Exception) {
                            Log.e("ExcludedIpHolder", "Failed to exclude route: $ipOrRange", e)
                        }
                    }
                }
            }
        }
    }
