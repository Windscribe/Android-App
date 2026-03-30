package com.windscribe.vpn.repository

import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.WgConnectConfig
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.WireguardUtil
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_UNABLE_TO_GENERATE_PSK
import com.windscribe.vpn.constants.NetworkErrorCodes.EXPIRED_OR_BANNED_ACCOUNT
import com.windscribe.vpn.exceptions.InvalidVPNConfigException
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing WireGuard VPN configuration lifecycle.
 *
 * Responsibilities:
 * - WireGuard key pair generation and secure caching
 * - Dynamic IP address allocation based on public key hashing
 * - Remote configuration assembly for tunnel establishment
 * - User account validation for connection authorization
 *
 * @property apiManager API interface for WireGuard initialization and session management
 * @property preferenceHelper Secure storage for WireGuard local parameters
 */
@Singleton
class WgConfigRepository @Inject constructor(
    private val apiManager: IApiCallManager,
    private val preferenceHelper: PreferencesHelper
) {
    private val logger = LoggerFactory.getLogger(TAG)
    private var skipCacheOnFirstInit = true

    /**
     * Retrieves complete WireGuard configuration for establishing a VPN connection.
     *
     * Flow:
     * 1. Validates user account status (optional)
     * 2. Generates or retrieves cached WireGuard keys
     * 3. Derives deterministic LAN IP from public key
     * 4. Assembles remote parameters with server configuration
     *
     * @param hostname Target server hostname for connection
     * @param serverPublicKey Server's WireGuard public key
     * @param forceInit Forces regeneration of keys, ignoring cache
     * @param checkUserAccountStatus Validates account status before proceeding
     * @return [CallResult.Success] with [WgRemoteParams] or [CallResult.Error]
     */
    suspend fun getWgParams(
        hostname: String,
        serverPublicKey: String,
        forceInit: Boolean = false,
        checkUserAccountStatus: Boolean = false,
        supportsV6: Boolean = false
    ): CallResult<WgRemoteParams> {
        // Validate user account if required
        if (checkUserAccountStatus) {
            validateUserAccountStatus().takeIf { it is CallResult.Error }?.let { error ->
                return error as CallResult.Error
            }
        }

        // Generate or retrieve local WireGuard parameters
        val localParams = generateKeys(forceInit).getOrElse { error ->
            logger.debug("Failed to generate keys (forceInit=$forceInit): ${error.errorMessage}")
            return error
        }

        // Derive public key from private key
        val userPublicKey = KeyPair(Key.fromBase64(localParams.privateKey))
            .publicKey
            .toBase64()

        logger.debug("Requesting WireGuard configuration for hostname: $hostname")

        // Generate LAN IP from public key
        val connectConfig = generateLanIpAddress(userPublicKey, localParams.hashedCIDR, localParams.hashedCIDRV6, supportsV6)
            .getOrElse { return it }

        return createRemoteParams(localParams, serverPublicKey, connectConfig)
    }

    /**
     * Clears all cached WireGuard parameters.
     *
     * Should be invoked on:
     * - User logout
     * - Connection reset
     * - Key rotation requirement
     */
    fun deleteKeys() {
        logger.debug("Clearing cached WireGuard parameters")
        preferenceHelper.wgLocalParams = null
    }

    /**
     * Unregisters the current WireGuard key with the server.
     * Forces re-initialization on next connection using the same key.
     */
    fun unregisterKey() {
        logger.debug("Unregister Wireguard key")
        preferenceHelper.wgRegisteredKey = null
        skipCacheOnFirstInit = true
    }

    /**
     * Generates or retrieves cached WireGuard key pair and PSK.
     *
     * Caching behavior:
     * - Returns cached params if available with valid hashedCIDR (unless forceInit=true)
     * - Reuses existing keypair if hashedCIDR is missing (refreshes server params only)
     * - Generates fresh keypair when forced or no cached params exist
     *
     * Additional features:
     * - Implements rate limiting delay before server initialization
     * - Automatic retry on PSK generation failures
     * - Persists hashedCIDR for deterministic IP allocation
     *
     * @param forceInit Bypasses cache and generates fresh keys
     * @return [CallResult.Success] with [WgLocalParams] or [CallResult.Error]
     */
    private suspend fun generateKeys(forceInit: Boolean): CallResult<WgLocalParams> {
        // Return cached params if available and not forcing init
        val cachedParams = preferenceHelper.wgLocalParams
        val isKeyRegisteredWithServer = preferenceHelper.wgRegisteredKey != null
        if (!forceInit && cachedParams?.hashedCIDR != null && cachedParams.hashedCIDRV6 != null
            && isKeyRegisteredWithServer && !skipCacheOnFirstInit) {
            logger.debug("Using cached WireGuard parameters")
            return CallResult.Success(cachedParams)
        }

        // Reuse existing key for re-registration (unless forceInit or no cached key)
        val keyPair = if (cachedParams != null && !forceInit) {
            logger.debug("Reusing existing WireGuard key pair")
            KeyPair(Key.fromBase64(cachedParams.privateKey))
        } else {
            logger.debug("Generating new WireGuard key pair")
            KeyPair()
        }
        skipCacheOnFirstInit = false
        val publicKey = keyPair.publicKey.toBase64()
        // Brief delay for rate limiting
        delay(INIT_DELAY_MS)

        // Initialize with server (with retry on PSK generation failure)
        val initResponse = performWgInitWithRetry(publicKey, forceInit)
            .getOrElse { return it }

        // Create and cache local params
        val privateKey = keyPair.privateKey.toBase64()
        return WgLocalParams(
            privateKey = privateKey,
            allowedIPs = initResponse.config.allowedIPs,
            allowedIPs6 = initResponse.config.allowedIPsV6,
            preSharedKey = initResponse.config.preSharedKey,
            hashedCIDR = initResponse.config.hashedCIDR,
            hashedCIDRV6 = initResponse.config.hashedCIDRV6
        ).also { params ->
            preferenceHelper.wgLocalParams = params
            // Save registered key after successful init
            preferenceHelper.wgRegisteredKey = privateKey
            logger.debug("Cached new WireGuard local parameters and registered key")
        }.let { CallResult.Success(it) }
    }

    /**
     * Performs WireGuard initialization with automatic retry on PSK failure.
     */
    private suspend fun performWgInitWithRetry(
        publicKey: String,
        forceInit: Boolean
    ): CallResult<WgInitResponse> {
        var response = apiManager.wgInit(publicKey, forceInit)

        // Retry once on PSK generation failure
        if (response.errorClass?.errorCode == ERROR_WG_UNABLE_TO_GENERATE_PSK) {
            logger.debug("Retrying WireGuard init due to PSK generation failure")
            response = apiManager.wgInit(publicKey, forceInit)
        }

        return result { response }
    }

    /**
     * Generates deterministic LAN IP addresses (IPv4 and optionally IPv6) from WireGuard public key.
     *
     * Uses SHA-256 hash of public key to derive IPs within CIDR ranges,
     * ensuring consistent IP allocation for the same public key.
     *
     * IPv6 generation is controlled by user preference and server capability:
     * - If ipv6Mode is "ipv4": only IPv4 is generated
     * - If ipv6Mode is "auto" AND server supports IPv6: both IPv4 and IPv6 are generated
     *
     * @param publicKey Base64-encoded WireGuard public key
     * @param hashedCIDR IPv4 CIDR range for IP allocation (e.g., ["100.64.0.0/10"])
     * @param hashedCIDRv6 IPv6 CIDR range for IP allocation (e.g., ["fd00::/8"])
     * @param supportsV6 Whether the server supports IPv6
     * @return [CallResult.Success] with [WgConnectConfig] containing IPv4 and optionally IPv6 addresses or [CallResult.Error]
     */
    private fun generateLanIpAddress(
        publicKey: String,
        hashedCIDR: List<String>?,
        hashedCIDRv6: List<String>?,
        supportsV6: Boolean
    ): CallResult<WgConnectConfig> {
        val cidr = hashedCIDR?.takeIf { it.isNotEmpty() }
            ?: throw InvalidVPNConfigException(CallResult.Error(errorMessage = "Invalid IPv4 CIDR range"))

        return runCatching {
            val ipv4Address = WireguardUtil.generateWireguardIP(publicKey, cidr[0])
            // Check if we should generate IPv6
            val ipv6Mode = preferenceHelper.ipv6Mode
            val shouldGenerateIPv6 = ipv6Mode == "auto" && supportsV6
            val (address, dns) = if (shouldGenerateIPv6) {
                val cidrV6 = hashedCIDRv6?.takeIf { it.isNotEmpty() }
                    ?: throw InvalidVPNConfigException(CallResult.Error(errorMessage = "Invalid IPv6 CIDR range"))
                val ipv6Address = WireguardUtil.generateWireguardIPv6(publicKey, cidrV6[0])
                val ipv6WithCIDR = "$ipv6Address/128"
                logger.debug("Generated IPv4: $ipv4Address, IPv6: $ipv6WithCIDR (mode: $ipv6Mode, server supports IPv6: $supportsV6 $publicKey)")
                Pair("$ipv4Address, $ipv6WithCIDR", DEFAULT_DNS_IPV4)
            } else {
                logger.debug("Generated IPv4 only: $ipv4Address (mode: $ipv6Mode, server supports IPv6: $supportsV6)")
                Pair(ipv4Address, DEFAULT_DNS_IPV4)
            }

            WgConnectConfig(address = address, dns = dns)
        }.fold(
            onSuccess = { CallResult.Success(it) },
            onFailure = { exception ->
                logger.error("IP generation failed: ${exception.message}", exception)
                CallResult.Error(errorMessage = "Failed to generate WireGuard IPs: ${exception.message}")
            }
        )
    }

    /**
     * Validates user account status for VPN access eligibility.
     *
     * @return [CallResult.Success] if account is active, [CallResult.Error] if expired/banned
     */
    private suspend fun validateUserAccountStatus(): CallResult<Unit> {
        logger.debug("Validating user account status")

        val backup = preferenceHelper.getBackupParameter()
        return result<UserSessionResponse> {
            apiManager.getSessionGeneric(null, backup = backup)
        }.fold(
            onSuccess = { session ->
                when (session.userAccountStatus) {
                    ACCOUNT_STATUS_ACTIVE -> {
                        CallResult.Success(Unit)
                    }
                    else -> {
                        logger.debug("Account validation failed - status: ${session.userAccountStatus}")
                        CallResult.Error(
                            code = EXPIRED_OR_BANNED_ACCOUNT,
                            errorMessage = "Account is expired or banned"
                        )
                    }
                }
            },
            onError = { error ->
                logger.debug("Failed to retrieve user session: ${error.errorMessage}")
                error
            }
        )
    }

    /**
     * Assembles complete remote parameters for WireGuard tunnel configuration.
     */
    private fun createRemoteParams(
        localParams: WgLocalParams,
        serverPublicKey: String,
        connectConfig: WgConnectConfig
    ): CallResult<WgRemoteParams> {
        // Determine allowedIPs based on whether IPv6 is configured
        val hasIPv6 = connectConfig.address.contains(",")
        val allowedIPs = if (hasIPv6) "${localParams.allowedIPs}, ${localParams.allowedIPs6}" else localParams.allowedIPs
        return WgRemoteParams(
            allowedIPs = allowedIPs,
            preSharedKey = localParams.preSharedKey,
            privateKey = localParams.privateKey,
            serverPublicKey = serverPublicKey,
            address = connectConfig.address,
            dns = connectConfig.dns
        ).also { params ->
            logger.debug("Assembled remote WireGuard parameters: {}", params)
        }.let { CallResult.Success(it) }
    }

    companion object {
        private const val TAG = "wg-config"
        private const val DEFAULT_DNS_IPV4 = "10.255.255.1"
        private const val INIT_DELAY_MS = 100L
        private const val ACCOUNT_STATUS_ACTIVE = 1
    }
}

/**
 * Remote WireGuard configuration parameters for tunnel establishment.
 *
 * @property allowedIPs IP ranges routed through VPN (typically "0.0.0.0/0")
 * @property preSharedKey Pre-shared key for post-quantum security
 * @property privateKey User's WireGuard private key
 * @property serverPublicKey Server's WireGuard public key
 * @property address Assigned LAN IP address within VPN subnet
 * @property dns DNS server IP for VPN queries
 */
data class WgRemoteParams(
    val allowedIPs: String,
    val preSharedKey: String,
    val privateKey: String,
    val serverPublicKey: String,
    val address: String,
    val dns: String
) {
    override fun toString(): String = buildString {
        append("WgRemoteParams(")
        append("allowedIPs='$allowedIPs', ")
        append("preSharedKey='${preSharedKey.maskSensitive()}', ")
        append("privateKey='${privateKey.maskSensitive()}', ")
        append("serverPublicKey='$serverPublicKey', ")
        append("address='$address', ")
        append("dns='$dns'")
        append(")")
    }

    private fun String.maskSensitive(visibleChars: Int = 8): String =
        if (length > visibleChars) "***${takeLast(visibleChars)}" else "***"
}

/**
 * Local WireGuard parameters cached for reuse across connections.
 *
 * @property privateKey User's WireGuard private key (base64)
 * @property allowedIPs IP ranges routed through VPN
 * @property preSharedKey Pre-shared key for added security layer
 * @property hashedCIDR CIDR range(s) for deterministic IP allocation (defaults to 100.64.0.0/10 for legacy compatibility)
 */
data class WgLocalParams(
    @SerializedName("privateKey")
    val privateKey: String,

    @SerializedName("allowedIPs")
    val allowedIPs: String,

    @SerializedName("allowedIPsV6")
    val allowedIPs6: String,

    @SerializedName("preSharedKey")
    val preSharedKey: String,

    @SerializedName("HashedCIDR")
    val hashedCIDR: List<String>?,

    @SerializedName("HashedCIDRv6")
    val hashedCIDRV6: List<String>?
) : Serializable
