/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VpnBackend
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.NetworkInfoListener
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.strongswan.android.logic.CharonVpnService
import org.strongswan.android.logic.VpnStateService
import org.strongswan.android.logic.VpnStateService.VpnStateListener
import java.io.File
import java.io.IOException

@Singleton
class IKev2VpnBackend(
        var scope: CoroutineScope,
        var networkInfoManager: NetworkInfoManager,
        var vpnStateManager: VPNConnectionStateManager,
        var serviceInteractor: ServiceInteractor,
        var protocolManager: ProtocolManager
) : VpnBackend(scope, vpnStateManager, serviceInteractor, protocolManager),
        VpnStateListener,
        NetworkInfoListener {

    private var vpnService: VpnStateService? = null
    private val stateServiceChannel = Channel<VpnStateService>()
    private var serviceConnection: ServiceConnection? = null
    override var active = false

    override fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean) {
        // stateChanged()
    }

    override fun activate() {
        bindToStateService()
        networkInfoManager.addNetworkInfoListener(this)
        active = true
        vpnLogger.debug("Ikev2 backend activated.")
    }

    override fun deactivate() {
        val context = Windscribe.appContext.applicationContext
        serviceConnection?.let {
            try {
                context.unbindService(it)
                serviceConnection = null
            } catch (e: Exception) {
            }
        }
        networkInfoManager.removeNetworkInfoListener(this)
        active = false
        vpnLogger.debug("Ikev2 backend deactivated.")
    }

    private suspend fun getVpnService() = vpnService ?: stateServiceChannel.receive()

    private fun bindToStateService() {
        val context = Windscribe.appContext.applicationContext
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                vpnService = (service as VpnStateService.LocalBinder).service.apply {
                    registerListener(this@IKev2VpnBackend)
                    scope.launch {
                        stateServiceChannel.send(this@apply)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                vpnService = null
            }
        }
        serviceConnection?.let {
            context.bindService(
                    Intent(context, VpnStateService::class.java),
                    it, Service.BIND_AUTO_CREATE
            )
        }
    }

    override fun connect() {
        vpnLogger.debug("Connecting to Ikev2 Service.")
        startConnectionJob()
        scope.launch {
            getVpnService().connect(null, true)
        }
    }

    override suspend fun disconnect() {
        connectionJob?.cancel()
        vpnLogger.debug("Disconnecting ikev2 service.")
        vpnService?.state?.let {
            getVpnService().disconnect()
        }
        delay(DISCONNECT_DELAY)
        deactivate()
    }

    override fun stateChanged() {
        vpnService?.let {
            vpnLogger.debug("Ikev2 Connection State: ${it.state}")
            if (it.state == VpnStateService.State.CONNECTED) {
                testConnectivity()
            } else {
                if (it.state != VpnStateService.State.DISCONNECTING) {
                    val state = serviceStateToVPNStatus(it.state, it.errorState)
                    state?.let {
                        updateState(state)
                    }
                }
            }
            checkLogFileSize()
        }
    }

    private fun checkLogFileSize(){
        val logFile = File(appContext.filesDir.absolutePath + File.separator + CharonVpnService.LOG_FILE)
        if (logFile.exists()) {
            try {
                val sizeInMb = logFile.length() / (1024 * 1024)
                if (sizeInMb > 1) {
                    logFile.delete()
                    logFile.createNewFile()
                }
            } catch (e: IOException) {
                vpnLogger.debug("Unable to create log file: $e")
            }
        }
    }

    private fun serviceStateToVPNStatus(state: VpnStateService.State, error: VpnStateService.ErrorState): VPNState? =
            if (error == VpnStateService.ErrorState.NO_ERROR) when (state) {
                VpnStateService.State.DISABLED -> {
                    connectionJob?.cancel()
                    VPNState(Disconnected)
                }
                VpnStateService.State.CONNECTING -> VPNState(VPNState.Status.Connecting)
                VpnStateService.State.CONNECTED -> VPNState(VPNState.Status.Connected)
                VpnStateService.State.DISCONNECTING -> VPNState(VPNState.Status.Disconnecting)
            } else {
                if (error == VpnStateService.ErrorState.AUTH_FAILED) {
                    authFailure = true
                }
                null
            }
}
