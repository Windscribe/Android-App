/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import com.google.gson.Gson
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_GENERATE_CREDENTIALS
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import dagger.Lazy
import io.reactivex.Completable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxSingle
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionDataRepository @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val autoConnectionManager: Lazy<AutoConnectionManager>
) {
    private val logger = LoggerFactory.getLogger("connection_data_updater")

    suspend fun updateConnectionData() {
        update().await()
    }

    fun update(): Completable {
        logger.debug("Starting connection data update...")
        return apiCallManager.getServerConfig().flatMap { response ->
            response.dataClass?.let {
                preferencesHelper.saveOpenVPNServerConfig(it)
            }
            if (errorRequestingCredentials(response.errorClass)) {
                return@flatMap rxSingle { appContext.vpnController.disconnectAsync() }.flatMap { apiCallManager.getServerCredentials() }
            } else {
                apiCallManager.getServerCredentials()
            }
        }
            .onErrorResumeNext(apiCallManager.getServerCredentials())
            .flatMap { response ->
                response.dataClass?.let {
                    preferencesHelper.saveCredentials(PreferencesKeyConstants.OPEN_VPN_CREDENTIALS,it)
                }
                if (errorRequestingCredentials(response.errorClass)) {
                    return@flatMap rxSingle { appContext.vpnController.disconnectAsync() }
                        .flatMap { apiCallManager.getServerCredentialsForIKev2() }
                } else {
                    apiCallManager.getServerCredentialsForIKev2()
                }
                apiCallManager.getServerCredentialsForIKev2()
            }.onErrorResumeNext(apiCallManager.getServerCredentialsForIKev2())
            .flatMap { response ->
                response.dataClass?.let {
                    preferencesHelper.saveCredentials(PreferencesKeyConstants.IKEV2_CREDENTIALS, it)
                }
                if (errorRequestingCredentials(response.errorClass)) {
                    return@flatMap rxSingle { appContext.vpnController.disconnectAsync() }
                        .flatMap { apiCallManager.getPortMap() }
                } else {
                    apiCallManager.getPortMap()
                }
                apiCallManager.getPortMap()
            }
            .onErrorResumeNext(apiCallManager.getPortMap())
            .flatMapCompletable {
                Completable.fromAction {
                    it.dataClass?.let {
                        saveSuggestedProtocolPort(it)
                        preferencesHelper.saveResponseStringData(
                            PreferencesKeyConstants.PORT_MAP,
                            Gson().toJson(it)
                        )
                        preferencesHelper.savePortMapVersion(NetworkKeyConstants.PORT_MAP_VERSION)
                    } ?: it.errorClass?.let {
                        logger.error(it.errorMessage)
                    }
                }
            }.doOnError { throwable: Throwable -> logger.debug(throwable.message) }
            .onErrorComplete()
    }

    private fun saveSuggestedProtocolPort(portMap: PortMapResponse) {
        portMap.suggested?.let {
            preferencesHelper.suggestedProtocol = it.protocol
            preferencesHelper.suggestedPort = it.port.toString()
            val indexOfSuggestedProtocol = autoConnectionManager.get().listOfProtocols.indexOfFirst { proto -> proto.protocol == portMap.suggested?.protocol }
            if (indexOfSuggestedProtocol != 0){
                autoConnectionManager.get().reset()
            }
        }
        if (portMap.suggested == null) {
            preferencesHelper.suggestedProtocol = null
            preferencesHelper.suggestedPort = null
        }
    }

    private fun errorRequestingCredentials(genericErrorResponse: ApiErrorResponse?): Boolean {
        return genericErrorResponse?.let {
            logger.debug("Failed to update server config." + it.errorMessage)
            if (it.errorCode == ERROR_UNABLE_TO_GENERATE_CREDENTIALS) {
                preferencesHelper.globalUserConnectionPreference = false
                true
            } else {
                false
            }
        } ?: false
    }
}
