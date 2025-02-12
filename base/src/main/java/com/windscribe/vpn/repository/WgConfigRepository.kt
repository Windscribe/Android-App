package com.windscribe.vpn.repository

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.WgConnectConfig
import com.windscribe.vpn.api.response.WgConnectResponse
import com.windscribe.vpn.api.response.WgInitResponse
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.ApiConstants.DEVICE_ID
import com.windscribe.vpn.constants.ApiConstants.HOSTNAME
import com.windscribe.vpn.constants.ApiConstants.WG_PUBLIC_KEY
import com.windscribe.vpn.constants.ApiConstants.WG_TTL
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_INVALID_PUBLIC_KEY
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_UNABLE_TO_GENERATE_PSK
import com.windscribe.vpn.constants.VpnPreferenceConstants
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class WgConfigRepository(val scope: CoroutineScope, val interactor: ServiceInteractor) {
    private val logger = LoggerFactory.getLogger("wg_config_repo")

    fun deleteKeys() {
        logger.debug("Deleting cached wg params.")
        interactor.preferenceHelper.wgLocalParams = null
    }

    private suspend fun generateKeys(forceInit: Boolean, protect:Boolean): CallResult<WgLocalParams> {
        return interactor.preferenceHelper.wgLocalParams?.let {
            logger.debug("Using existing wg public key.")
            return@let CallResult.Success(it)
        } ?: run {
            logger.debug("Generating new wg key pair.")
            val keyPair = KeyPair()
            val publicKey = keyPair.publicKey.toBase64()
            val paramsMap = mutableMapOf(Pair(WG_PUBLIC_KEY, publicKey))
            if (forceInit) {
                logger.debug("Generating wg params with force_init=1")
                paramsMap["force_init"] = "1"
            }
            val callResult = interactor.apiManager.wgInit(publicKey, forceInit)
                    .flatMap {
                        when (it.errorClass?.errorCode) {
                            ERROR_WG_UNABLE_TO_GENERATE_PSK -> {
                                logger.debug("Retrying wg init Error: wg utility failure.")
                                interactor.apiManager.wgInit(publicKey, forceInit)
                            }
                            else -> {
                                Single.just(it)
                            }
                        }
                    }.delaySubscription(100, TimeUnit.MILLISECONDS)
                    .result<WgInitResponse>()
           when(callResult){
                is CallResult.Success -> {
                    val localParams = WgLocalParams(keyPair.privateKey.toBase64(), callResult.data.config.allowedIPs, callResult.data.config.preSharedKey)
                    interactor.preferenceHelper.wgLocalParams = localParams
                    CallResult.Success(localParams)
                }
                is CallResult.Error -> callResult
            }
        }
    }

    suspend fun getWgParams(hostname: String, serverPublicKey: String, forceInit: Boolean = false, checkUserAccountStatus: Boolean = false): CallResult<WgRemoteParams> {
       if(checkUserAccountStatus){
           logger.debug("Checking user status.")
           val userSessionResponse = interactor.apiManager.getSessionGeneric(null).result<UserSessionResponse>()
           if(userSessionResponse is CallResult.Success && userSessionResponse.data.userAccountStatus!=1){
               logger.debug("User status is expired/banned. ${userSessionResponse.data.userAccountStatus}")
               return CallResult.Error(NetworkErrorCodes.EXPIRED_OR_BANNED_ACCOUNT, "User account banned or expired.")
           }
           if(userSessionResponse is CallResult.Error){
               logger.debug("Error getting user session ${userSessionResponse.errorMessage}.")
               return userSessionResponse
           }
       }

        val wgInitResponse = generateKeys(forceInit,checkUserAccountStatus)
        if (wgInitResponse is CallResult.Success) {
            // Connect
            val userPublicKey = KeyPair(Key.fromBase64(wgInitResponse.data.privateKey)).publicKey.toBase64()
            logger.debug("Request Wg connect for $hostname")
            val wgConnectResponse = wgConnect(hostname, userPublicKey,checkUserAccountStatus)
            if (wgConnectResponse is CallResult.Success) {
                val remoteParams = WgRemoteParams(wgInitResponse.data.allowedIPs, wgInitResponse.data.preSharedKey, wgInitResponse.data.privateKey, serverPublicKey, wgConnectResponse.data.address, wgConnectResponse.data.dns)
                logger.debug(remoteParams.toString())
                return CallResult.Success(remoteParams)
            }
            if (wgConnectResponse is CallResult.Error && wgConnectResponse.code != ERROR_WG_INVALID_PUBLIC_KEY) {
                return wgConnectResponse
            }
            // Re Init
            if (wgConnectResponse is CallResult.Error && wgConnectResponse.code == ERROR_WG_INVALID_PUBLIC_KEY) {
                logger.debug("Wg connect failed clearing keys and running re-init.")
                val reInitResponse = reInit(hostname, forceInit,checkUserAccountStatus)
                if (reInitResponse is CallResult.Success) {
                    val remoteParams = WgRemoteParams(reInitResponse.data.first.allowedIPs, reInitResponse.data.first.preSharedKey, reInitResponse.data.first.privateKey, serverPublicKey, reInitResponse.data.second.address, reInitResponse.data.second.dns)
                    logger.debug(remoteParams.toString())
                    return CallResult.Success(remoteParams)
                }
                if (reInitResponse is CallResult.Error) {
                    return reInitResponse
                }
            }
        }
        // Wg init failed
        if (wgInitResponse is CallResult.Error) {
            logger.debug("Error generating key forceInit $forceInit ${wgInitResponse.errorMessage}")
            return wgInitResponse
        }
        return CallResult.Error(errorMessage = "Failed get wg params with expected error")
    }

    private suspend fun reInit(hostname: String, forceInit: Boolean, protect: Boolean): CallResult<Pair<WgLocalParams, WgConnectConfig>> {
        deleteKeys()
        return when (val wgInitResponse = generateKeys(forceInit,protect)){
            is CallResult.Success -> {
                val userPublicKey = KeyPair(Key.fromBase64(wgInitResponse.data.privateKey)).publicKey.toBase64()
                return when(val wgConnectResponse = wgConnect(hostname, userPublicKey,protect)){
                    is CallResult.Success -> CallResult.Success(Pair(wgInitResponse.data, wgConnectResponse.data))
                    is CallResult.Error -> wgConnectResponse
                }
            }
             is CallResult.Error -> wgInitResponse
        }
    }

    private suspend fun wgConnect(hostname: String, userPublicKey: String, protect: Boolean): CallResult<WgConnectConfig> {
        val params = mutableMapOf(
            Pair(HOSTNAME, hostname),
            Pair(WG_PUBLIC_KEY, userPublicKey),
            Pair(WG_TTL, VpnPreferenceConstants.WG_CONNECT_DEFAULT_TTL)
        )
        var deviceId = ""
        if (interactor.preferenceHelper.isConnectingToStaticIp) {
            runCatching {
                return@runCatching interactor.preferenceHelper.getDeviceUUID()
                    ?: throw Exception("Failed to get username.")
            }.onSuccess {
                logger.debug("Adding device id to wg connect $it")
                deviceId = it
            }
        }
        val callResult = interactor.apiManager.wgConnect(userPublicKey, hostname, deviceId)
            .flatMap {
                if (it.errorClass?.errorCode == ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP) {
                    logger.debug("Retrying wg connect Error: Unable to selected wg ip.")
                    interactor.apiManager.wgConnect(userPublicKey, hostname, deviceId)
                } else {
                    Single.just(it)
                }
            }.delaySubscription(100, TimeUnit.MILLISECONDS)
            .result<WgConnectResponse>()
        return when (callResult) {
            is CallResult.Success -> {
                CallResult.Success(callResult.data.config)
            }
            is CallResult.Error -> callResult
        }
    }
}

data class WgRemoteParams(val allowedIPs: String, val preSharedKey: String, val privateKey: String, val serverPublicKey: String, val address: String, val dns: String)
data class WgLocalParams(
    @SerializedName("privateKey")
    @Expose
    val privateKey: String,
    @SerializedName("allowedIPs")
    @Expose
    val allowedIPs: String,
    @SerializedName("preSharedKey")
    @Expose
    val preSharedKey: String
) : Serializable