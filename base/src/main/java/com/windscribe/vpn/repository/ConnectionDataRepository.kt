/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import com.google.gson.Gson
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_GENERATE_CREDENTIALS
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import dagger.Lazy
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionDataRepository @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val autoConnectionManager: Lazy<AutoConnectionManager>
) {
    private val logger = LoggerFactory.getLogger("data")

    suspend fun update(): Boolean {
        logger.debug("Starting connection data update...")

        val result = try {
            // Step 1: Get server config
            val serverConfigResult = result<String> {
                apiCallManager.getServerConfig()
            }
            when (serverConfigResult) {
                is CallResult.Success -> {
                    preferencesHelper.saveOpenVPNServerConfig(serverConfigResult.data)
                }

                is CallResult.Error -> {
                    if (serverConfigResult.code == ERROR_UNABLE_TO_GENERATE_CREDENTIALS) {
                        preferencesHelper.globalUserConnectionPreference = false
                        appContext.vpnController.disconnectAsync()
                    }
                    logger.debug("Failed to get server config: ${serverConfigResult.errorMessage}")
                }
            }

            // Step 2: Get OpenVPN credentials
            val openVpnCredsResult = result<ServerCredentialsResponse> {
                apiCallManager.getServerCredentials()
            }
            when (openVpnCredsResult) {
                is CallResult.Success -> {
                    preferencesHelper.saveCredentials(
                        PreferencesKeyConstants.OPEN_VPN_CREDENTIALS,
                        openVpnCredsResult.data
                    )
                }

                is CallResult.Error -> {
                    if (openVpnCredsResult.code == ERROR_UNABLE_TO_GENERATE_CREDENTIALS) {
                        preferencesHelper.globalUserConnectionPreference = false
                        appContext.vpnController.disconnectAsync()
                    }
                    logger.debug("Failed to get OpenVPN credentials: ${openVpnCredsResult.errorMessage}")
                }
            }

            // Step 3: Get IKEv2 credentials
            val ikev2CredsResult = result<ServerCredentialsResponse> {
                apiCallManager.getServerCredentialsForIKev2()
            }
            when (ikev2CredsResult) {
                is CallResult.Success -> {
                    preferencesHelper.saveCredentials(
                        PreferencesKeyConstants.IKEV2_CREDENTIALS,
                        ikev2CredsResult.data
                    )
                }

                is CallResult.Error -> {
                    if (ikev2CredsResult.code == ERROR_UNABLE_TO_GENERATE_CREDENTIALS) {
                        preferencesHelper.globalUserConnectionPreference = false
                        appContext.vpnController.disconnectAsync()
                    }
                    logger.debug("Failed to get IKEv2 credentials: ${ikev2CredsResult.errorMessage}")
                }
            }

            // Step 4: Get port map
            val portMapResult = result<PortMapResponse> {
                apiCallManager.getPortMap()
            }
            when (portMapResult) {
                is CallResult.Success -> {
                    saveSuggestedProtocolPort(portMapResult.data)
                    preferencesHelper.portMap = Gson().toJson(portMapResult.data)
                    preferencesHelper.savePortMapVersion(NetworkKeyConstants.PORT_MAP_VERSION)
                }

                is CallResult.Error -> {
                    logger.error("Failed to get port map: ${portMapResult.errorMessage}")
                }
            }

            CallResult.Success(Unit)
        } catch (e: Exception) {
            logger.debug("Connection data update error: ${e.message}")
            CallResult.Error(-1, e.message ?: "Unknown error")
        }
        return result is CallResult.Success
    }

    private fun saveSuggestedProtocolPort(portMap: PortMapResponse) {
        portMap.suggested?.let {
            preferencesHelper.suggestedProtocol = it.protocol
            preferencesHelper.suggestedPort = it.port.toString()
            val indexOfSuggestedProtocol =
                autoConnectionManager.get().listOfProtocols.indexOfFirst { proto -> proto.protocol == portMap.suggested?.protocol }
            if (indexOfSuggestedProtocol != 0) {
                autoConnectionManager.get().reset()
            }
        }
        if (portMap.suggested == null) {
            preferencesHelper.suggestedProtocol = null
            preferencesHelper.suggestedPort = null
        }
    }
}
