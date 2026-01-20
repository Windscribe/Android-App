/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import com.windscribe.vpn.R.layout
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.alert.showAlertDialog
import com.windscribe.vpn.alert.showErrorDialog
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.state.DynamicShortcutManager
import com.windscribe.vpn.state.DynamicShortcutManager.Companion.QUICK_CONNECT_ACTION_KEY
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
    private val logger = LoggerFactory.getLogger("vpn")

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var vpnBackendHolder: VpnBackendHolder

    @Inject
    lateinit var locationRepository: LocationRepository

    lateinit var protocolInformation: ProtocolInformation

    lateinit var connectionId: UUID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_launch)
        appContext.activityComponent.inject(this)
        if (intent.hasExtra("protocolInformation")) {
            protocolInformation =
                    intent.getSerializableExtra("protocolInformation") as ProtocolInformation
            connectionId = intent.getSerializableExtra("connectionId") as UUID
            askForPermission()
        }  else {
            finish()
            val action = intent.getStringExtra(QUICK_CONNECT_ACTION_KEY)
            when (action) {
                DynamicShortcutManager.QUICK_CONNECT_ACTION -> {
                    appContext.preference.globalUserConnectionPreference = true
                    vpnController.connectAsync()
                }
                DynamicShortcutManager.RECENT_CONNECT_ACTION -> {
                    val connectId = intent.getIntExtra(DynamicShortcutManager.RECENT_CONNECT_ID, -1)
                    locationRepository.setSelectedCity(connectId)
                    setupLocationTypeInt()
                    appContext.preference.globalUserConnectionPreference = true
                    vpnController.connectAsync()
                }
                else -> {
                    appContext.preference.globalUserConnectionPreference = false
                    vpnController.disconnectAsync()
                }
            }
        }
    }

    private fun setupLocationTypeInt(){
        val locationTypeInt = intent.getIntExtra(DynamicShortcutManager.RECENT_LOCATION_TYPE_INT, 0)
        when(locationTypeInt) {
            1 -> {
                appContext.preference.isConnectingToStaticIp = true
                appContext.preference.isConnectingToConfigured = false
            }
            2 -> {
                appContext.preference.isConnectingToStaticIp = false
                appContext.preference.isConnectingToConfigured = true
            }
            else -> {
                appContext.preference.isConnectingToStaticIp = false
                appContext.preference.isConnectingToConfigured = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == RESULT_OK) {
                logger.info("User granted VPN Permission.")
                if (isNotificationsEnabled()) {
                    vpnBackendHolder.connect(protocolInformation, connectionId)
                    finish()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissions(
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION
                        )
                    } else {
                        vpnBackendHolder.connect(protocolInformation, connectionId)
                        finish()
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                logger.info("User denied VPN permission.")
                showErrorDialog(this,
                    "Windscribe requires VPN permission to configure VPN. " +
                            "Sometimes you may see this error if another VPN app is configured as 'Always on VPN'. " +
                            "Please turn off 'Always on' in all profiles.") {
                    scope.launch {
                        try {
                            vpnController.disconnectAsync()
                        } catch (e: Exception) {
                            logger.error("Failed to disconnect VPN: ${e.message}")
                        } finally {
                            finish()
                        }
                    }
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION) {
            vpnBackendHolder.connect(protocolInformation, connectionId)
            finish()
        }
    }

    private fun isNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (checkCallingPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        } else {
            true
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
        } catch (_: InterruptedException) {
        } catch (_: IOException) {
        }
    }

    companion object {
        private const val START_VPN_PROFILE = 70
        private const val NOTIFICATION = 71
    }
}
