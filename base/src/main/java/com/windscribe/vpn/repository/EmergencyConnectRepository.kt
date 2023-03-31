package com.windscribe.vpn.repository

import com.google.common.hash.Hashing
import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.model.OpenVPNConnectionInfo
import okhttp3.internal.toImmutableList
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.net.Inet4Address
import java.nio.charset.Charset
import java.util.*

interface EmergencyConnectRepository {
    suspend fun getConnectionInfo(): Result<List<OpenVPNConnectionInfo>>
}

/**
 * Implementation of emergency connect repository.
 */
class EmergencyConnectRepositoryImpl : EmergencyConnectRepository {
    private val logger = LoggerFactory.getLogger("e_connect_r")

    init {
        logger.debug("Initializing Emergency connect repository.")
    }

    /** Builds e-connect domain [buildEConnectDomain]
     *
     * Resolves e-connect domain using system resolver. [resolveDomain]
     *
     * Adds hardcoded ips to the list
     *
     * Creates two vpn profiles for each ip in the list (udp 443, tcp 443)
     *
     * @return
    List of [OpenVPNConnectionInfo]
     */
    override suspend fun getConnectionInfo(): Result<List<OpenVPNConnectionInfo>> {
        if (BuildConfig.EMERGENCY_IP1.isEmpty() || BuildConfig.EMERGENCY_IP2.isEmpty() || BuildConfig.EMERGENCY_USERNAME.isEmpty() || BuildConfig.EMERGENCY_PASSWORD.isEmpty() || BuildConfig.BACKUP_API_ENDPOINT_STRING.isEmpty()) {
            return Result.failure(WindScribeException("Emergency connect credentials not available."))
        }
        return resolveDomain(buildEConnectDomain()).mapCatching {
            val list = it.toMutableList()
            list.add(BuildConfig.EMERGENCY_IP1)
            list.add(BuildConfig.EMERGENCY_IP2)
            return@mapCatching list.toImmutableList()
        }.recoverCatching {
            logger.debug("Failed to resolve e-connect domain.")
            return@recoverCatching listOf<String>(
                BuildConfig.EMERGENCY_IP1, BuildConfig.EMERGENCY_IP2
            )
        }.mapCatching {
            val inputStream = appContext.assets.open("emergency.ovpn")
            val reader = InputStreamReader(inputStream)
            val serverConfig = reader.readText()
            logger.debug("Building VPN Profiles.")
            it.map { ip ->
                listOf(
                    OpenVPNConnectionInfo(
                        serverConfig,
                        ip,
                        "443",
                        "udp",
                        BuildConfig.EMERGENCY_USERNAME,
                        BuildConfig.EMERGENCY_PASSWORD
                    ), OpenVPNConnectionInfo(
                        serverConfig,
                        ip,
                        "443",
                        "tcp",
                        BuildConfig.EMERGENCY_USERNAME,
                        BuildConfig.EMERGENCY_PASSWORD
                    )
                )
            }.reduce { l1, l2 ->
                l1.plus(l2)
            }
        }
    }

    private fun buildEConnectDomain(): String {
        logger.debug("Building e-connect domain.")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val cohort = (1..3).random()
        val calenderInfo = "${calendar.get(Calendar.MONTH) + 1}${calendar.get(Calendar.YEAR)}"
        val hash = Hashing.sha1().hashString(
            "${BuildConfig.BACKUP_API_ENDPOINT_STRING}$cohort$calenderInfo",
            Charset.defaultCharset()
        )
        return "econnect.$hash.com"
    }

    private fun resolveDomain(domain: String): Result<List<String>> {
        logger.debug("Resolving e-connect domain.")
        return kotlin.runCatching {
            Inet4Address.getAllByName(domain).map {
                return@map it.hostAddress ?: throw Exception("Empty host address.")
            }.shuffled()
        }
    }
}