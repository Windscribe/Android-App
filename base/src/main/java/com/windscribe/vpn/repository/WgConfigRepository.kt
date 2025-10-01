package com.windscribe.vpn.repository

import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.WgConnectConfig
import com.windscribe.vpn.api.response.WgConnectResponse
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.api.response.WgRekeyResponse
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.wireguard.WireGuardVpnProfile
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_UNABLE_TO_GENERATE_PSK
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.jvm.optionals.getOrNull

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
     * Tracks PSK state for a specific server hostname.
     *
     * @property hostname The server hostname
     * @property currentPsk The PSK that should currently work for this server
     * @property previousPsk The previous PSK to retry if currentPsk fails
     * @property lastHandshakeTime When the last successful handshake occurred with this server
     */
    private data class PerServerPskState(
        val hostname: String,
        val currentPsk: String,
        val previousPsk: String?,
        val lastHandshakeTime: Long
    )

    /**
     * Global PSK state tracking the latest rotated PSK and per-server states.
     *
     * @property latestPsk The most recently rotated PSK from the server
     * @property timestamp When the latest PSK was generated
     * @property perServerStates Map of hostname to their specific PSK states
     */
    private data class GlobalPskState(
        val latestPsk: String,
        val timestamp: Long,
        val perServerStates: MutableMap<String, PerServerPskState> = mutableMapOf()
    )

    private var globalPskState: GlobalPskState? = null
        get() {
            if (field == null) {
                field = loadGlobalPskState()
            }
            return field
        }
        set(value) {
            field = value
            saveGlobalPskState(value)
        }

    private val pskRotationThresholdMs = 5 * 60 * 1000L
    private val peerExpiryThresholdMs = 60 * 60 * 1000L // 1 hour - server removes inactive peers

    /**
     * Deletes all cached WireGuard keys and PSK state.
     * Called when user logs out or resets connection.
     */
    fun deleteKeys() {
        logger.debug("Deleting cached WireGuard parameters")
        interactor.preferenceHelper.wgLocalParams = null
        globalPskState = null
    }

    init {
        scope.launch {
            rotatePskOnAppStart()
        }
    }

    /**
     * Retrieves the hostname from the current WireGuard profile configuration.
     * Looks up the server IP from the profile and maps it to a hostname using the city database.
     *
     * @return The hostname if found, null otherwise
     */
    private suspend fun getHostname(): String? {
        val configContent = Util.getProfile<WireGuardVpnProfile>()?.content
        val ip = WireGuardVpnProfile.getHostName(configContent) ?: return null
        return localDbInterface.getCitiesAsync().asSequence()
            .flatMap { it.nodes }
            .firstOrNull { it.ip3 == ip }?.hostname
    }

    /**
     * Extracts the pre-shared key from the current WireGuard profile configuration.
     *
     * @return The PSK in base64 format if found, null otherwise
     */
    private fun getProfilePsk(): String? {
        val configContent = Util.getProfile<WireGuardVpnProfile>()?.content ?: return null
        return WireGuardVpnProfile.createConfigFromString(configContent)
            .peers.firstOrNull()?.preSharedKey?.getOrNull()?.toBase64()
    }

    /**
     * Rotates the PSK on the server by calling the rekey API.
     * Note: Cached server peers will continue using the old PSK until they expire (~1 hour of inactivity).
     *
     * @return CallResult containing the new PSK or an error
     */
    private suspend fun rotatePsk(): CallResult<WgRekeyResponse> {
        val privateKey = interactor.preferenceHelper.wgLocalParams?.privateKey!!
        val publicKey = KeyPair(Key.fromBase64(privateKey)).publicKey.toBase64()
        return interactor.apiManager.wgRekey(publicKey).result()
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
                    // Initialize global PSK state with the new PSK
                    val currentTime = System.currentTimeMillis()
                    globalPskState = GlobalPskState(
                        latestPsk = callResult.data.config.preSharedKey,
                        timestamp = currentTime
                    )

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
                // Get the PSK to use for this server
                val pskToUse = getPskForServer(hostname)
                when (val wgConnectResponse = wgConnect(hostname, userPublicKey)) {
                    is CallResult.Success -> {
                        createRemoteParams(
                            wgInitResponse.data.copy(preSharedKey = pskToUse),
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

    /// Gets PSK for server.
    private suspend fun getPskForServer(hostname: String): String {
        // Clean up expired states before selecting PSK
        cleanupExpiredServerStates()

        val state = globalPskState
        val localParams = interactor.preferenceHelper.wgLocalParams
        if (state == null || localParams == null) {
            logger.debug("No global PSK state, using current local PSK")
            return localParams?.preSharedKey ?: ""
        }

        // Check if we have per-server state for this hostname
        val serverState = state.perServerStates[hostname]
        val isRetry = nextHostnameToTry() != null
        if (serverState != null) {
            // For retry use current psk
            if (isRetry) {
                logger.debug("Using current PSK for $hostname: ${serverState.previousPsk?.takeLast(8)}")
                return serverState.currentPsk
            }
            // Use previous PSK
            val pskToUse = serverState.previousPsk ?: serverState.currentPsk
            logger.debug(
                "Using ${if (serverState.previousPsk != null) "previous" else "current"} PSK for $hostname: ${
                    pskToUse.takeLast(
                        8
                    )
                }"
            )
            return pskToUse
        }
        // No per-server state yet, meaning we've never connected to this server
        // Use the latest global PSK since the server has no cached peer for our public key
        logger.debug(
            "No per-server state for $hostname, using latest global PSK: ${
                state.latestPsk.takeLast(
                    8
                )
            }"
        )
        return state.latestPsk
    }

    /// Updates server PSK state.
    private fun updateServerPskState(hostname: String, currentPsk: String, previousPsk: String?) {
        val state = globalPskState ?: return
        val serverState = PerServerPskState(
            hostname = hostname,
            currentPsk = currentPsk,
            previousPsk = previousPsk,
            lastHandshakeTime = System.currentTimeMillis()
        )
        state.perServerStates[hostname] = serverState
        globalPskState = state
        logger.debug(
            "Updated PSK state for $hostname: current=${currentPsk.takeLast(8)}, previous=${
                previousPsk?.takeLast(
                    8
                )
            }"
        )
    }

    /**
     * Called when a successful handshake is detected for the current connection.
     * Updates the server's PSK state to record which PSK was successfully used.
     */
    suspend fun onHandshakeSuccess() {
        val hostname = getHostname() ?: run {
            logger.debug("Cannot update PSK state on handshake: hostname not found")
            return
        }

        val state = globalPskState
        val localParams = interactor.preferenceHelper.wgLocalParams

        if (state == null || localParams == null) {
            logger.debug("Cannot update PSK state on handshake: no global state or local params")
            return
        }

        // Get the current server state to see which PSK was used
        val serverState = state.perServerStates[hostname]

        // Determine which PSK was actually used in the connection
        val usedPsk = if (serverState != null) {
            // We had state - use the PSK that was selected (previousPsk if retry, else currentPsk)
            serverState.previousPsk ?: serverState.currentPsk
        } else {
            // First connection to this server - we would have used the latest global PSK
            state.latestPsk
        }

        // Always update state to refresh lastHandshakeTime and clear previousPsk if needed
        val shouldLog = serverState == null || serverState.previousPsk != null
        if (shouldLog) {
            logger.debug("Handshake successful for $hostname with PSK: ${usedPsk.takeLast(8)}")
        }
        updateServerPskState(hostname, usedPsk, null)
    }

    /**
     * Called when a PSK failure is detected for a specific hostname.
     * Updates the server's PSK state to use the latest global PSK for next connection attempt.
     */
    fun onPskFailure(hostname: String) {
        val state = globalPskState
        if (state == null) {
            logger.debug("Cannot update PSK state on failure: no global state")
            return
        }

        val serverState = state.perServerStates[hostname]
        val currentPsk = serverState?.currentPsk ?: state.latestPsk

        // Mark the current PSK as failed and prepare to use the latest global PSK
        logger.debug(
            "PSK failed for $hostname, switching to latest global PSK: ${
                state.latestPsk.takeLast(
                    8
                )
            }"
        )
        updateServerPskState(hostname, state.latestPsk, currentPsk)
    }

    /**
     * Determines if the current hostname should be retried with a different PSK.
     * Used by auto-connection logic to detect if PSK rotation can resolve connection issues.
     *
     * @return The hostname if a different PSK is available to try, null otherwise
     */
    suspend fun nextHostnameToTry(): String? {
        val hostname = getHostname() ?: return null
        val triedPsk = getProfilePsk() ?: return null
        val state = globalPskState ?: return null
        val serverState = state.perServerStates[hostname] ?: return null
        // Return hostname only if there's a different PSK to try
        return if (serverState.currentPsk != triedPsk) hostname else null
    }

    /**
     * Cleans up per-server PSK states for servers that haven't had handshake in over 1 hour.
     * Server-side peers expire after ~1 hour of inactivity, so these states are stale.
     */
    private fun cleanupExpiredServerStates() {
        val state = globalPskState ?: return
        val currentTime = System.currentTimeMillis()
        val expiredHostnames = state.perServerStates.filter { (_, serverState) ->
            (currentTime - serverState.lastHandshakeTime) > peerExpiryThresholdMs
        }.keys

        if (expiredHostnames.isNotEmpty()) {
            expiredHostnames.forEach { hostname ->
                state.perServerStates.remove(hostname)
            }
            globalPskState = state
            logger.debug("Cleaned up ${expiredHostnames.size} expired server states: ${expiredHostnames.joinToString()}")
        }
    }

    private suspend fun rotatePskOnAppStart() {
        val params = interactor.preferenceHelper.wgLocalParams
        if (params == null) {
            logger.debug("No local params found, skipping PSK rotation on app start")
            return
        }

        val shouldRotate = shouldRotatePsk()
        if (shouldRotate) {
            val rotateResult = rotatePsk()
            when (rotateResult) {
                is CallResult.Success -> {
                    val newPsk = rotateResult.data.config.presharedKey
                    val currentTime = System.currentTimeMillis()

                    // Update global PSK state with the new latest PSK
                    val existingState = globalPskState
                    globalPskState = if (existingState != null) {
                        GlobalPskState(
                            latestPsk = newPsk,
                            timestamp = currentTime,
                            perServerStates = existingState.perServerStates
                        )
                    } else {
                        GlobalPskState(
                            latestPsk = newPsk,
                            timestamp = currentTime
                        )
                    }

                    logger.debug("PSK rotated on app start: ${newPsk.takeLast(8)}")
                }

                is CallResult.Error -> {
                    logger.debug("PSK rotation failed: ${rotateResult.errorMessage}")
                }
            }
        }
    }

    private fun shouldRotatePsk(): Boolean {
        val currentTime = System.currentTimeMillis()
        val state = globalPskState
        val online = WindUtilities.isOnline()
        if (!online) {
            return false
        }
        return when {
            state == null -> {
                logger.debug("No previous PSK state found - rotation required")
                true
            }

            (currentTime - state.timestamp) > pskRotationThresholdMs -> {
                // Check if the current latestPsk has been used by any server
                val isPskUsed = state.perServerStates.values.any { serverState ->
                    serverState.currentPsk == state.latestPsk || serverState.previousPsk == state.latestPsk
                }

                if (!isPskUsed) {
                    val timeElapsed = (currentTime - state.timestamp) / 1000
                    logger.debug("Skipping PSK rotation - latest PSK (${state.latestPsk.takeLast(8)}) not yet used by any server (${timeElapsed}s elapsed)")
                    return false
                }

                val timeElapsed = (currentTime - state.timestamp) / 1000
                logger.debug("PSK rotation threshold exceeded (${timeElapsed}s elapsed)")
                true
            }

            else -> {
                val timeElapsed = (currentTime - state.timestamp) / 1000
                logger.debug("PSK within threshold (${timeElapsed}s elapsed)")
                false
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

    private fun saveGlobalPskState(state: GlobalPskState?) {
        try {
            val json = state?.let { Gson().toJson(it) } ?: ""
            interactor.preferenceHelper.saveResponseStringData("wg_global_psk_state", json)
        } catch (e: Exception) {
            logger.debug("Failed to save global PSK state: ${e.message}")
        }
    }

    private fun loadGlobalPskState(): GlobalPskState? {
        return try {
            val json = interactor.preferenceHelper.getResponseString("wg_global_psk_state")
            if (json.isNullOrEmpty()) null else Gson().fromJson(json, GlobalPskState::class.java)
        } catch (e: Exception) {
            logger.debug("Failed to load global PSK state: ${e.message}")
            null
        }
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