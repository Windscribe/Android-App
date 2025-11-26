package com.windscribe.vpn.repository

import android.util.Base64
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.WgConnectConfig
import com.windscribe.vpn.api.response.WgConnectResponse
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_UNABLE_TO_GENERATE_PSK
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.security.MessageDigest
import javax.inject.Singleton

/**
 * Repository for loading and managing WireGuard configuration
 * This repository handles the complete lifecycle of WireGuard connections including:
 * - Key pair generation and management
 * - Connection configuration assembly
 */
@Singleton
class WgConfigRepository(
    private val apiManager: IApiCallManager,
    private val preferenceHelper: PreferencesHelper
) {
    private val logger = LoggerFactory.getLogger("wg-config")

    /**
     * Deletes all cached WireGuard keys and PSK state.
     * Called when user logs out or resets connection.
     */
    fun deleteKeys() {
        logger.debug("Deleting cached WireGuard parameters")
        preferenceHelper.wgLocalParams = null
    }
    /**
     * Generates or retrieves WireGuard key pair and initial PSK.
     * If keys already exist, returns cached params. Otherwise generates new keypair and calls wgInit API.
     *
     * @param forceInit If true, forces generation of new keys even if cached params exist
     * @return CallResult containing local WireGuard parameters (private key, allowed IPs, PSK)
     */
    private suspend fun generateKeys(forceInit: Boolean): CallResult<WgLocalParams> {
        return preferenceHelper.wgLocalParams?.let { existingParams ->
            return@let CallResult.Success(existingParams)
        } ?: run {
            logger.debug("Generating new WireGuard key pair")
            val keyPair = KeyPair()
            val publicKey = keyPair.publicKey.toBase64()

            // Initial delay
            delay(100)

            // Try wgInit with retry logic
            var response = apiManager.wgInit(publicKey, forceInit)
            if (response.errorClass?.errorCode == ERROR_WG_UNABLE_TO_GENERATE_PSK) {
                logger.debug("Retrying WireGuard init - Error: WireGuard utility failure")
                response = apiManager.wgInit(publicKey, forceInit)
            }

            val callResult = result<WgInitResponse> {
                response
            }

            when (callResult) {
                is CallResult.Success -> {
                    val localParams = WgLocalParams(
                        keyPair.privateKey.toBase64(),
                        callResult.data.config.allowedIPs,
                        callResult.data.config.preSharedKey
                    )
                    preferenceHelper.wgLocalParams = localParams
                    CallResult.Success(localParams)
                }

                is CallResult.Error -> callResult
            }
        }
    }

    /**
     * Gets the complete WireGuard configuration needed to establish a connection.
     * This is the main entry point for retrieving WireGuard parameters.
     *
     * @param hostname The server hostname to connect to
     * @param serverPublicKey The server's WireGuard public key
     * @param forceInit If true, forces regeneration of keys even if cached
     * @param checkUserAccountStatus If true, validates user account status before proceeding
     * @return CallResult containing complete WireGuard parameters or an error
     */
    suspend fun getWgParams(
        hostname: String,
        serverPublicKey: String,
        forceInit: Boolean = false,
        checkUserAccountStatus: Boolean = false
    ): CallResult<WgRemoteParams> {
        if (checkUserAccountStatus) {
            val userStatusResult = validateUserAccountStatus()
            if (userStatusResult is CallResult.Error) {
                return userStatusResult
            }
        }

        return when (val wgInitResponse =
            generateKeys(forceInit)) {
            is CallResult.Success -> {
                val userPublicKey =
                    KeyPair(Key.fromBase64(wgInitResponse.data.privateKey)).publicKey.toBase64()
                logger.debug("Requesting WireGuard connect for $hostname")
                val wgConnectResponse = getWireGuardLANIPFromPublicKey(userPublicKey)
                when (wgConnectResponse) {
                    is CallResult.Success -> {
                        createRemoteParams(
                            wgInitResponse.data,
                            serverPublicKey,
                            wgConnectResponse.data
                        )
                    }

                    is CallResult.Error -> {
                        wgConnectResponse
                    }
                }
            }

            is CallResult.Error -> {
                logger.debug("Error generating keys (forceInit=$forceInit): ${wgInitResponse.errorMessage}")
                wgInitResponse
            }
        }
    }

    private fun getWireGuardLANIPFromPublicKey(wgPublicKey: String): CallResult<WgConnectConfig>{
        val pub: ByteArray = try {
            Base64.decode(wgPublicKey, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid base64 public key: $wgPublicKey", e)
            return CallResult.Error(errorMessage = "Failed to decode base64 public key: $wgPublicKey")
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(pub)
        val v = ((digest[0].toInt() and 0xFF) shl 24) or
                ((digest[1].toInt() and 0xFF) shl 16) or
                ((digest[2].toInt() and 0xFF) shl 8) or
                (digest[3].toInt() and 0xFF)
        val maskedV = v and 0x003FFFFF
        val octet2 = 64 or ((maskedV shr 16) and 0xFF)
        val octet3 = (maskedV shr 8) and 0xFF
        val octet4 = maskedV and 0xFF
        return CallResult.Success(WgConnectConfig("100.$octet2.$octet3.$octet4", "10.255.255.1"))
    }

    /// Validates user account status
    private suspend fun validateUserAccountStatus(): CallResult<Unit> {
        logger.debug("Checking user account status")
        return when (val userSessionResponse = result<UserSessionResponse> {
            apiManager.getSessionGeneric(null)
        }) {
            is CallResult.Success -> {
                if (userSessionResponse.data.userAccountStatus != 1) {
                    logger.debug("User account is expired/banned: ${userSessionResponse.data.userAccountStatus}")
                    CallResult.Error(
                        NetworkErrorCodes.EXPIRED_OR_BANNED_ACCOUNT,
                        "User account banned or expired"
                    )
                } else {
                    CallResult.Success(Unit)
                }
            }

            is CallResult.Error -> {
                logger.debug("Error getting user session: ${userSessionResponse.errorMessage}")
                userSessionResponse
            }
        }
    }

    private fun createRemoteParams(
        localParams: WgLocalParams,
        serverPublicKey: String,
        connectConfig: WgConnectConfig
    ): CallResult<WgRemoteParams> {
        val remoteParams = WgRemoteParams(
            localParams.allowedIPs,
            localParams.preSharedKey,
            localParams.privateKey,
            serverPublicKey,
            connectConfig.address,
            connectConfig.dns
        )
        logger.debug("Created WireGuard remote params: {}", remoteParams)
        return CallResult.Success(remoteParams)
    }
}

data class WgRemoteParams(
    val allowedIPs: String,
    val preSharedKey: String,
    val privateKey: String,
    val serverPublicKey: String,
    val address: String,
    val dns: String
) {
    override fun toString(): String {
        return "WgRemoteParams(allowedIPs='$allowedIPs', preSharedKey='${preSharedKey.takeLast(8)}', privateKey='${
            privateKey.takeLast(8)
        }', serverPublicKey='$serverPublicKey', address='$address', dns='$dns')"
    }
}

data class WgLocalParams(
    @SerializedName("privateKey")
    @Expose
    val privateKey: String,
    @SerializedName("allowedIPs")
    @Expose
    val allowedIPs: String,
    @SerializedName("preSharedKey")
    @Expose
    var preSharedKey: String
) : Serializable