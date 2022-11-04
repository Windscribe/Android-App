/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import com.windscribe.vpn.R.layout
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.state.VPNConnectionStateManager
import de.blinkt.openvpn.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import javax.inject.Inject

class VPNPermissionActivity : Activity() {

    private var cmFixed = false
    private val logger = LoggerFactory.getLogger("vpn_backend")

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var vpnBackendHolder: VpnBackendHolder

    lateinit var protocolInformation: ProtocolInformation

    lateinit var connectionId: UUID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_launch)
        Windscribe.appContext.activityComponent.inject(this)
        protocolInformation =
            intent.getSerializableExtra("protocolInformation") as ProtocolInformation
        connectionId = intent.getSerializableExtra("connectionId") as UUID
        askForPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == RESULT_OK) {
                logger.debug("User granted VPN Permission.")
                vpnBackendHolder.connect(protocolInformation, connectionId)
                finish()
            } else if (resultCode == RESULT_CANCELED) {
                logger.debug("User denied VPN permission.")
                scope.launch {
                    vpnController.disconnectAsync()
                    finish()
                }
            }
        }
    }

    private fun askForPermission() {
        val intent: Intent? = try {
            VpnService.prepare(this)
        } catch (e: Exception) {
            logger.info(e.toString())
            scope.launch { vpnController.disconnectAsync() }
            return
        }
        fixDevTun()
        if (intent != null) {
            try {
                startActivityForResult(intent, START_VPN_PROFILE)
            } catch (ane: ActivityNotFoundException) {
                scope.launch { vpnController.disconnectAsync() }
                logger.debug("Device image does not support vpn.")
            }
        } else {
            logger.info("Already has VPN permission.")
            onActivityResult(START_VPN_PROFILE, RESULT_OK, null)
        }
    }

    private fun fixDevTun() {
        // Check if we want to fix /dev/tun
        val prefs = Preferences.getDefaultSharedPreferences(this)
        val useCM9Fix = prefs.getBoolean("useCM9Fix", false)
        val loadTunModule = prefs.getBoolean("loadTunModule", false)
        if (loadTunModule) {
            executesSCommand("insmod /system/lib/modules/tun.ko")
        }
        if (useCM9Fix && !cmFixed) {
            executesSCommand("chown system /dev/tun")
        }
    }

    private fun executesSCommand(command: String) {
        try {
            val pb = ProcessBuilder("su", "-c", command)
            val p = pb.start()
            val ret = p.waitFor()
            if (ret == 0) {
                cmFixed = true
            }
        } catch (e: InterruptedException) {
        } catch (e: IOException) {
        }
    }

    companion object {
        private const val START_VPN_PROFILE = 70
    }
}
