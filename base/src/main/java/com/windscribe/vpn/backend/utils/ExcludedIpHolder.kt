package com.windscribe.vpn.backend.utils

import android.net.VpnService
import android.os.Build
import android.util.Log
import com.windscribe.vpn.localdatabase.ExcludedIpDomainDao
import com.windscribe.vpn.localdatabase.tables.ExcludedIpDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExcludedIpHolder
    @Inject
    constructor(
        private val excludedIpDomainDao: ExcludedIpDomainDao,
    ) {
        private val logger = LoggerFactory.getLogger("vpn")
        private var excludedIps: List<String> = emptyList()

        suspend fun resolveAndStore() {
            excludedIps =
                withContext(Dispatchers.IO) {
                    try {
                        val entries = excludedIpDomainDao.getAll()
                        val resolvedIps = mutableListOf<String>()

                        entries.forEach { entry ->
                            when (entry.type) {
                                ExcludedIpDomain.EntryType.IP -> {
                                    resolvedIps.add(entry.value)
                                    logger.debug("Added IP to exclude list: ${entry.value}")
                                }
                                ExcludedIpDomain.EntryType.IP_RANGE -> {
                                    resolvedIps.add(entry.value)
                                    logger.debug("Added IP range to exclude list: ${entry.value}")
                                }
                                ExcludedIpDomain.EntryType.HOSTNAME -> {
                                    try {
                                        val addresses = InetAddress.getAllByName(entry.value)
                                        addresses.forEach { address ->
                                            val ip = address.hostAddress
                                            if (ip != null) {
                                                resolvedIps.add(ip)
                                                logger.debug("Resolved hostname ${entry.value} to IP: $ip")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        logger.error("Failed to resolve hostname ${entry.value}: ${e.message}")
                                    }
                                }
                            }
                        }

                        logger.info("Total excluded IPs/ranges: ${resolvedIps.size}")
                        resolvedIps
                    } catch (e: Exception) {
                        logger.error("Failed to get excluded IP ranges: ${e.message}")
                        emptyList()
                    }
                }
        }

        fun applyExcludedRoutes(builder: VpnService.Builder) {
            applyExcludedRoutes(builder, excludedIps)
        }

        fun clear() {
            excludedIps = emptyList()
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
                            if (ipOrRange.contains("/")) {
                                val parts = ipOrRange.split("/")
                                builder.excludeRoute(parts[0], parts[1].toInt())
                            } else {
                                builder.excludeRoute(ipOrRange, 32)
                            }
                        } catch (e: Exception) {
                            Log.e("ExcludedIpHolder", "Failed to exclude route: $ipOrRange", e)
                        }
                    }
                }
            }
        }
    }
