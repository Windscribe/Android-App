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
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_INVALID_PUBLIC_KEY
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

@Singleton
class WgConfigRepository(
    private val interactor: ServiceInteractor,
    private val scope: CoroutineScope,
    private val localDbInterface: LocalDbInterface
) {
    private val logger = LoggerFactory.getLogger("wg-config")

    private data class PskTrackingInfo(
        val hostname: String,
        val timestamp: Long
    )

    private var currentPskInfo: PskTrackingInfo? = null
        get() {
            if (field == null) {
                field = loadPskTrackingInfo()
            }
            return field
        }
        set(value) {
            field = value
            savePskTrackingInfo(value)
        }

    private val pskRotationThresholdMs = 5 * 60 * 1000L

    fun deleteKeys() {
        logger.debug("Deleting cached WireGuard parameters")
        interactor.preferenceHelper.wgLocalParams = null
        currentPskInfo = null
    }

    init {
        scope.launch {
            rotatePskOnAppStart()
        }
    }

    private suspend fun getHostname(): String? {
        val configContent = Util.getProfile<WireGuardVpnProfile>()?.content
        val ip = WireGuardVpnProfile.getHostName(configContent)
        if (ip == null) {
            return null
        }
        return localDbInterface.getCitiesAsync().asSequence().flatMap { it.nodes }.asSequence()
            .firstOrNull { it.ip3 == ip }?.hostname
    }

    private suspend fun rotatePsk(): CallResult<WgRekeyResponse> {
        val privateKey = interactor.preferenceHelper.wgLocalParams?.privateKey!!
        val publicKey = KeyPair(Key.fromBase64(privateKey)).publicKey.toBase64()
        return interactor.apiManager.wgRekey(publicKey).result()
    }

    private suspend fun generateKeys(
        forceInit: Boolean,
        hostname: String,
        updatingPeerIp: Boolean = false
    ): CallResult<WgLocalParams> {
        return interactor.preferenceHelper.wgLocalParams?.let { existingParams ->
            currentPskInfo = PskTrackingInfo(hostname, System.currentTimeMillis())
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
                    currentPskInfo = PskTrackingInfo(hostname, System.currentTimeMillis())
                    CallResult.Success(localParams)
                }

                is CallResult.Error -> callResult
            }
        }
    }

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
            generateKeys(forceInit, hostname, checkUserAccountStatus)) {
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
                        if (wgConnectResponse.code == ERROR_WG_INVALID_PUBLIC_KEY) {
                            logger.debug("WireGuard connect failed, clearing keys and re-initializing")
                            handleReinitialization(hostname, forceInit, serverPublicKey)
                        } else {
                            wgConnectResponse
                        }
                    }
                }
            }

            is CallResult.Error -> {
                logger.debug("Error generating keys (forceInit=$forceInit): ${wgInitResponse.errorMessage}")
                wgInitResponse
            }
        }
    }

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

    private suspend fun handleReinitialization(
        hostname: String,
        forceInit: Boolean,
        serverPublicKey: String
    ): CallResult<WgRemoteParams> {
        return when (val reInitResponse = reInit(hostname, forceInit)) {
            is CallResult.Success -> {
                createRemoteParams(
                    reInitResponse.data.first,
                    serverPublicKey,
                    reInitResponse.data.second
                )
            }

            is CallResult.Error -> reInitResponse
        }
    }

    private suspend fun reInit(
        hostname: String,
        forceInit: Boolean
    ): CallResult<Pair<WgLocalParams, WgConnectConfig>> {
        deleteKeys()
        return when (val wgInitResponse = generateKeys(forceInit, hostname)) {
            is CallResult.Success -> {
                val userPublicKey =
                    KeyPair(Key.fromBase64(wgInitResponse.data.privateKey)).publicKey.toBase64()
                when (val wgConnectResponse = wgConnect(hostname, userPublicKey)) {
                    is CallResult.Success -> {
                        CallResult.Success(Pair(wgInitResponse.data, wgConnectResponse.data))
                    }

                    is CallResult.Error -> wgConnectResponse
                }
            }

            is CallResult.Error -> wgInitResponse
        }
    }

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


    private suspend fun rotatePskOnAppStart() {
        val params = interactor.preferenceHelper.wgLocalParams
        if (params == null) return
        val hostname = getHostname() ?: return
        val shouldRotate = shouldRotatePsk(hostname)
        if (shouldRotate) {
            val rotateResult = rotatePsk()
            when (rotateResult) {
                is CallResult.Success -> {
                    params.preSharedKey = rotateResult.data.config.presharedKey
                    interactor.preferenceHelper.wgLocalParams = params
                    currentPskInfo = PskTrackingInfo(hostname, System.currentTimeMillis())
                    logger.debug("PSK rotated on App start: ${rotateResult.data.config.presharedKey.takeLast(8)}")
                }

                is CallResult.Error -> {
                    logger.debug("PSK rotation failed: ${rotateResult.errorMessage}")
                }
            }
        }
    }

    private fun shouldRotatePsk(hostname: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val pskInfo = currentPskInfo
        val online = WindUtilities.isOnline()
        if (!online) {
            return false
        }
        return when {
            pskInfo == null -> {
                logger.debug("No previous PSK info found - rotation required")
                true
            }
            pskInfo.hostname != hostname -> {
                true
            }

            (currentTime - pskInfo.timestamp) > pskRotationThresholdMs -> {
                (currentTime - pskInfo.timestamp) / 1000
                true
            }

            else -> {
                val timeElapsed = (currentTime - pskInfo.timestamp) / 1000
                logger.debug("${interactor.preferenceHelper.wgLocalParams?.preSharedKey?.takeLast(8)} within threshold (${timeElapsed}s elapsed)")
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

    private fun savePskTrackingInfo(pskInfo: PskTrackingInfo?) {
        try {
            val json = if (pskInfo != null) {
                Gson().toJson(pskInfo)
            } else {
                null
            }
            interactor.preferenceHelper.saveResponseStringData("wg_psk_tracking_info", json ?: "")
        } catch (e: Exception) {
            logger.debug("Failed to save PSK tracking info: ${e.message}")
        }
    }

    private fun loadPskTrackingInfo(): PskTrackingInfo? {
        return try {
            val json = interactor.preferenceHelper.getResponseString("wg_psk_tracking_info")
            if (json.isNullOrEmpty()) {
                null
            } else {
                Gson().fromJson(json, PskTrackingInfo::class.java)
            }
        } catch (e: Exception) {
            logger.debug("Failed to load PSK tracking info: ${e.message}")
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
        return "WgRemoteParams(allowedIPs='$allowedIPs', preSharedKey='${preSharedKey.takeLast(8)}', privateKey='${privateKey.takeLast(8)
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