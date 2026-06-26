/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.windscribe.vpn.R.drawable
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.state.ShortcutStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

@AndroidEntryPoint
class VpnTileService : TileService() {
    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var scope: CoroutineScope

    private var job: Job? = null

    @Inject
    lateinit var shortcutStateManager: ShortcutStateManager

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    private val logger = LoggerFactory.getLogger("shortcut")

    override fun onCreate() {
        super.onCreate()
    }

    override fun onClick() {
        super.onClick()
        logger.debug("Quick tile icon clicked....")
        if (vpnConnectionStateManager.isVPNActive()) {
            preferencesHelper.globalUserConnectionPreference = false
            vpnController.disconnectAsync()
        } else {
            shortcutStateManager.connect()
        }
    }

    override fun onStartListening() {
        shortcutStateManager.load {
            job =
                scope.launch {
                    vpnConnectionStateManager.state.collectLatest {
                        if (it.status == Disconnected && it.error?.error == VPNState.ErrorType.UserReconnect) {
                            return@collectLatest
                        }
                        resetState(it.status)
                    }
                }
        }
    }

    override fun onStopListening() {
        job?.cancel()
    }

    private fun resetState(status: VPNState.Status) {
        qsTile ?: return
        when (status) {
            Connected -> {
                logger.debug("Changing quick tile status to Connected")
                setTileState(getIcon(), Tile.STATE_ACTIVE)
            }

            Disconnected -> {
                logger.debug("Changing quick tile status to Disconnected")
                setTileState(getIcon(), Tile.STATE_INACTIVE)
            }

            Connecting -> {
                logger.debug("Changing quick tile status to Connecting")
                setTileState(getIcon(true), Tile.STATE_ACTIVE)
            }

            VPNState.Status.Disconnecting,
            VPNState.Status.RequiresUserInput,
            VPNState.Status.UnsecuredNetwork,
            VPNState.Status.InvalidSession -> {
                logger.debug("Changing quick tile status to Disconnecting/Inactive: $status")
                setTileState(getIcon(), Tile.STATE_INACTIVE)
            }

            VPNState.Status.ProtocolSwitch -> {
                logger.debug("Changing quick tile status to ProtocolSwitch (showing as Connecting)")
                setTileState(getIcon(true), Tile.STATE_ACTIVE)
            }
        }
    }

    private fun setTileState(
        icon: Int,
        tileState: Int,
    ) {
        try {
            qsTile?.icon = Icon.createWithResource(this, icon)
            qsTile?.state = tileState
            qsTile.updateTile()
        } catch (ignored: Exception) {
        }
    }

    private fun getIcon(isConnecting: Boolean = false): Int = if (isConnecting) drawable.ic_tile_connecting else drawable.ic_tile_connect
}
