package com.windscribe.vpn.repository

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.WgConnectConfig
import com.windscribe.vpn.api.response.WgConnectResponse
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_UNABLE_TO_GENERATE_PSK
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Repository for loading and managing WireGuard configuration with intelligent PSK rotation.
 *
 * This repository handles the complete lifecycle of WireGuard connections including:
 * - Key pair generation and management
 * - Pre-shared key (PSK) rotation and state tracking per server
 * - Connection configuration assembly
 * - Automatic PSK rotation on app start (every 5 minutes)
 * - PSK retry logic when connection fails
 *
 * ## PSK Rotation Strategy
 * The repository implements a two-PSK approach per server:
 * - **currentPsk**: The PSK that should work for this server
 * - **previousPsk**: The previous PSK to try if currentPsk fails
 * - **latestPsk**: The globally rotated PSK from the server
 *
 * When a server's cached peer expires (after ~1 hour of inactivity), it needs the latest global PSK.
 * This system intelligently tracks which PSK each server expects based on connection success/failure.
 */
@Singleton
class WgConfigRepository(
    private val interactor: ServiceInteractor,
    scope: CoroutineScope,
    private val localDbInterface: LocalDbInterface
) {
    private val logger = LoggerFactory.getLogger("wg-config")

    /**
     * Deletes all cached WireGuard keys and PSK state.
     * Called when user logs out or resets connection.
     */
    fun deleteKeys() {
        logger.debug("Deleting cached WireGuard parameters")
        interactor.preferenceHelper.wgLocalParams = null
    }
    /**
     * Generates or retrieves WireGuard key pair and initial PSK.
     * If keys already exist, returns cached params. Otherwise generates new keypair and calls wgInit API.
     *
     * @param forceInit If true, forces generation of new keys even if cached params exist
     * @return CallResult containing local WireGuard parameters (private key, allowed IPs, PSK)
     */
    private suspend fun generateKeys(forceInit: Boolean): CallResult<WgLocalParams> {
        return interactor.preferenceHelper.wgLocalParams?.let { existingParams ->
            return@let CallResult.Success(existingParams)
        } ?: run {
            logger.debug("Generating new WireGuard key pair")
            val keyPair = KeyPair()
            val publicKey = keyPair.publicKey.toBase64()
            val callResult = interactor.apiManager.wgInit(publicKey, forceInit)
                .flatMap { response ->
                    when (response.errorClass?.errorCode) {
                        ERROR_WG_UNABLE_TO_GENERATE_PSK -> {
                            logger.debug("Retrying WireGuard init - Error: WireGuard utility failure")
                            interactor.apiManager.wgInit(publicKey, forceInit)
                        }

                        else -> Single.just(response)
                    }
                }
                .delaySubscription(100, TimeUnit.MILLISECONDS)
                .result<WgInitResponse>()

            when (callResult) {
                is CallResult.Success -> {
                    val localParams = WgLocalParams(
                        keyPair.privateKey.toBase64(),
                        callResult.data.config.allowedIPs,
                        callResult.data.config.preSharedKey
                    )
                    interactor.preferenceHelper.wgLocalParams = localParams
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
                when (val wgConnectResponse = wgConnect(hostname, userPublicKey)) {
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

    /// Validates user account status
    private suspend fun validateUserAccountStatus(): CallResult<Unit> {
        logger.debug("Checking user account status")
        return when (val userSessionResponse =
            interactor.apiManager.getSessionGeneric(null).result<UserSessionResponse>()) {
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

    /// Pulls second half of wireguard configuration(Address, DNS)
    private suspend fun wgConnect(
        hostname: String,
        userPublicKey: String
    ): CallResult<WgConnectConfig> {
        val deviceId = getDeviceIdForStaticIp()

        val callResult = interactor.apiManager.wgConnect(userPublicKey, hostname, deviceId)
            .flatMap { response ->
                if (response.errorClass?.errorCode == ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP) {
                    logger.debug("Retrying WireGuard connect - Error: Unable to select WireGuard IP")
                    interactor.apiManager.wgConnect(userPublicKey, hostname, deviceId)
                } else {
                    Single.just(response)
                }
            }
            .delaySubscription(100, TimeUnit.MILLISECONDS)
            .result<WgConnectResponse>()

        return when (callResult) {
            is CallResult.Success -> CallResult.Success(callResult.data.config)
            is CallResult.Error -> callResult
        }
    }

    /// Generates device id for static ip
    private fun getDeviceIdForStaticIp(): String {
        return if (interactor.preferenceHelper.isConnectingToStaticIp) {
            runCatching {
                interactor.preferenceHelper.getDeviceUUID()
                    ?: throw Exception("Failed to get device UUID")
            }.getOrElse { exception ->
                logger.debug("Error getting device UUID: ${exception.message}")
                ""
            }.also { deviceId ->
                if (deviceId.isNotEmpty()) {
                    logger.debug("Adding device ID to WireGuard connect: $deviceId")
                }
            }
        } else {
            ""
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