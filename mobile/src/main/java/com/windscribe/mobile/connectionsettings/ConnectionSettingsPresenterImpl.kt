/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionsettings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Build
import androidx.core.content.ContextCompat
import com.windscribe.mobile.R
import com.windscribe.mobile.utils.PermissionManager
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.ActivityInteractorImpl.PortMapLoadCallback
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.PortMapResponse.PortMap
import com.windscribe.vpn.commonutils.Ext.getFakeTrafficVolumeOptions
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants.CONNECTION_MODE_AUTO
import com.windscribe.vpn.constants.PreferencesKeyConstants.CONNECTION_MODE_MANUAL
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_STEALTH
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_TCP
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_UDP
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WS_TUNNEL
import com.windscribe.vpn.decoytraffic.FakeTrafficVolume
import com.windscribe.vpn.mocklocation.MockLocationManager.Companion.isAppSelectedInMockLocationList
import com.windscribe.vpn.mocklocation.MockLocationManager.Companion.isDevModeOn
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

class ConnectionSettingsPresenterImpl @Inject constructor(
        private var connSettingsView: ConnectionSettingsView,
        private var interactor: ActivityInteractor,
        private val permissionManager: PermissionManager
) : ConnectionSettingsPresenter {
    private val logger = LoggerFactory.getLogger("con_settings_p")
    private var currentPoint = 1500
    override fun onStart() {
        //Set split tunnel text view
        if (interactor.getAppPreferenceInterface().splitTunnelToggle) {
            //Split tunnel is on
            logger.info("Split tunnel settings is ON")
            connSettingsView.setSplitTunnelText(
                    interactor.getResourceString(R.string.on),
                    interactor.getThemeColor(R.attr.wdActionColor)
            )
        } else {
            logger.info("Split tunnel settings is OFF")
            connSettingsView.setSplitTunnelText(
                    interactor.getResourceString(R.string.off),
                    interactor.getThemeColor(R.attr.wdSecondaryColor)
            )
        }
        setAutoStartMenu()
        if (interactor.getAppPreferenceInterface().lanByPass) {
            connSettingsView.setLanBypassToggle(R.drawable.ic_toggle_button_on)
        } else {
            connSettingsView.setLanBypassToggle(R.drawable.ic_toggle_button_off)
        }
        if (interactor.getAppPreferenceInterface().isDecoyTrafficOn) {
            connSettingsView.setDecoyTrafficToggle(R.drawable.ic_toggle_button_on)
        } else {
            connSettingsView.setDecoyTrafficToggle(R.drawable.ic_toggle_button_off)
        }
        setDecoyTrafficParameters()
        if (interactor.getAppPreferenceInterface().isAntiCensorshipOn) {
            connSettingsView.setAntiCensorshipToggle(R.drawable.ic_toggle_button_on)
        } else {
            connSettingsView.setAntiCensorshipToggle(R.drawable.ic_toggle_button_off)
        }
    }

    override fun onDestroy() {
        interactor.getDecoyTrafficController().load()
        if (interactor.getCompositeDisposable().isDisposed.not()) {
            logger.info("Disposing observer...")
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun init() {
        setupLayoutBasedOnConnectionMode()
        setUpAutoModePorts()
        setupPacketSizeMode()
        setUpKeepAlive()
    }

    override fun onAllowLanClicked() {
        if (interactor.getAppPreferenceInterface().lanByPass) {
            connSettingsView.setLanBypassToggle(R.drawable.ic_toggle_button_off)
            interactor.getAppPreferenceInterface().lanByPass = false
            logger.info("Setting lan bypass to true")
        } else {
            connSettingsView.setLanBypassToggle(R.drawable.ic_toggle_button_on)
            interactor.getAppPreferenceInterface().lanByPass = true
            logger.info("Setting lan bypass to false")
        }
    }

    override fun onAutoFillPacketSizeClicked() {
        mtuSizeFromNetworkInterface
    }

    override fun onAutoStartOnBootClick() {
        if (interactor.getAppPreferenceInterface().autoStartOnBoot) {
            connSettingsView.setAutoStartOnBootToggle(R.drawable.ic_toggle_button_off)
            interactor.getAppPreferenceInterface().autoStartOnBoot = false
            logger.info("Setting auto start on boot to false")
        } else {
            connSettingsView.setAutoStartOnBootToggle(R.drawable.ic_toggle_button_on)
            interactor.getAppPreferenceInterface().autoStartOnBoot = true
            logger.info("Setting auto start on boot to true")
        }
    }

    override fun onDecoyTrafficClick() {
        if (interactor.getAppPreferenceInterface().isDecoyTrafficOn) {
            connSettingsView.setDecoyTrafficToggle(R.drawable.ic_toggle_button_off)
            interactor.getAppPreferenceInterface().isDecoyTrafficOn = false
            logger.info("Setting decoy traffic to false")
            interactor.getDecoyTrafficController().stop()
        } else {
            connSettingsView.showExtraDataUseWarning()
        }
    }

    override fun turnOnDecoyTraffic() {
        connSettingsView.setDecoyTrafficToggle(R.drawable.ic_toggle_button_on)
        interactor.getAppPreferenceInterface().isDecoyTrafficOn = true
        logger.info("Setting decoy traffic to true")
        if (interactor.getVpnConnectionStateManager().isVPNConnected()) {
            interactor.getDecoyTrafficController().load()
            interactor.getDecoyTrafficController().start()
        }
    }

    private fun setDecoyTrafficParameters() {
        val multiplierOptions = getFakeTrafficVolumeOptions()
        val lowerLimit =
                interactor.getAppPreferenceInterface().fakeTrafficVolume.name
        connSettingsView.setupFakeTrafficVolumeAdapter(lowerLimit, multiplierOptions)
        resetPotentialTrafficInfo()
    }

    override fun onFakeTrafficVolumeSelected(label: String) {
        interactor.getAppPreferenceInterface().fakeTrafficVolume =
                FakeTrafficVolume.valueOf(label)
        resetPotentialTrafficInfo()
    }

    override fun onConnectionModeAutoClicked() {
        //Save connection mode to preference only if  manual mode is selected
        if (CONNECTION_MODE_AUTO != interactor.getSavedConnectionMode()) {
            interactor.saveConnectionMode(CONNECTION_MODE_AUTO)
            interactor.getAppPreferenceInterface().setChosenProtocol(null)
            setUpAutoModePorts()
            interactor.getAutoConnectionManager().reset()
        }
    }

    override fun onConnectionModeManualClicked() {
        //Save connection mode to preference only if a different connection mode is selected
        if (CONNECTION_MODE_MANUAL != interactor.getSavedConnectionMode()) {
            interactor.saveConnectionMode(CONNECTION_MODE_MANUAL)
            connSettingsView.setKeepAliveContainerVisibility(interactor.getAppPreferenceInterface().savedProtocol == PROTO_IKev2)
        }
    }

    override fun onGpsSpoofingClick() {
        permissionManager.withBackgroundLocationPermission { error ->
            if (error != null) {
                logger.debug(error)
            } else {
                onPermissionProvided()
            }
        }
    }

    override fun onHotStart() {
        setGpsSpoofingMenu()
    }

    override fun onKeepAliveAutoModeClicked() {
        val keepAliveSizeModeAuto =
                interactor.getAppPreferenceInterface().isKeepAliveModeAuto
        if (!keepAliveSizeModeAuto) {
            interactor.getAppPreferenceInterface().isKeepAliveModeAuto = true
            connSettingsView.setKeepAliveModeAdapter(
                    interactor.getResourceString(R.string.auto),
                    arrayOf(
                            interactor.getResourceString(R.string.auto),
                            interactor.getResourceString(R.string.manual)
                    )
            )
        }
    }

    override fun onKeepAliveManualModeClicked() {
        val keepAliveSizeModeAuto =
                interactor.getAppPreferenceInterface().isKeepAliveModeAuto
        if (keepAliveSizeModeAuto) {
            setKeepAlive(interactor.getAppPreferenceInterface().keepAlive)
            interactor.getAppPreferenceInterface().isKeepAliveModeAuto = false
            connSettingsView.setKeepAliveModeAdapter(
                    interactor.getResourceString(R.string.manual),
                    arrayOf(
                            interactor.getResourceString(R.string.auto),
                            interactor.getResourceString(R.string.manual)
                    )
            )
        }
    }

    override fun onManualLayoutSetupCompleted() {
        logger.info("Manual layout setup is completed...")
        setProtocolAdapter()
    }

    override fun onPacketSizeAutoModeClicked() {
        val packetSizeModeAuto =
                interactor.getAppPreferenceInterface().isPackageSizeModeAuto
        if (!packetSizeModeAuto) {
            interactor.getAppPreferenceInterface().setPacketSizeModeToAuto(true)
        }
    }

    override fun onPacketSizeManualModeClicked() {
        val packetSizeModeAuto =
                interactor.getAppPreferenceInterface().isPackageSizeModeAuto
        if (packetSizeModeAuto) {
            interactor.getAppPreferenceInterface().setPacketSizeModeToAuto(false)
        }
    }

    override fun onPermissionProvided() {
        if (isAppSelectedInMockLocationList(appContext)
                && isDevModeOn(appContext)
        ) {
            if (interactor.getAppPreferenceInterface().isGpsSpoofingOn) {
                connSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_off)
                interactor.getAppPreferenceInterface().setGpsSpoofing(false)
                logger.info("Setting gps spoofing to true")
            } else {
                connSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_on)
                interactor.getAppPreferenceInterface().setGpsSpoofing(true)
                logger.info("Setting gps spoofing to false")
            }
        } else {
            connSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_off)
            interactor.getAppPreferenceInterface().setGpsSpoofing(false)
            connSettingsView.openGpsSpoofSettings()
        }
    }

    override fun onPortSelected(heading: String, port: String) {
        logger.info("Saving selected port...")
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                when (getProtocolFromHeading(portMapResponse, heading)) {
                    PROTO_IKev2 -> {
                        logger.info("Saving selected IKev2 port...")
                        interactor.getAppPreferenceInterface().saveIKEv2Port(port)
                    }

                    PROTO_UDP -> {
                        logger.info("Saving selected udp port...")
                        interactor.saveUDPPort(port)
                    }

                    PROTO_TCP -> {
                        logger.info("Saving selected tcp port...")
                        interactor.saveTCPPort(port)
                    }

                    PROTO_STEALTH -> {
                        logger.info("Saving selected stealth port...")
                        interactor.saveSTEALTHPort(port)
                    }

                    PROTO_WS_TUNNEL -> {
                        logger.info("Saving selected ws tunnel port...")
                        interactor.saveWSTunnelPort(port)
                    }

                    PROTO_WIRE_GUARD -> {
                        logger.info("Saving selected wire guard port...")
                        interactor.getAppPreferenceInterface().saveWireGuardPort(port)
                    }

                    else -> {
                        logger.info("Saving default port (udp)...")
                        interactor.saveUDPPort(port)
                    }
                }
                interactor.getAutoConnectionManager().reset()
            }
        })
    }

    override fun onProtocolSelected(heading: String) {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                val protocol = getProtocolFromHeading(portMapResponse, heading)
                val savedProtocol = interactor.getSavedProtocol()
                if (savedProtocol == protocol) {
                    //Do nothing
                    logger.info("Protocol re-selected is same as saved. No action taken...")
                } else {
                    logger.info("Saving selected protocol...")
                    interactor.saveProtocol(protocol)
                    setPortMapAdapter(heading)
                    interactor.getAutoConnectionManager().reset()
                }
            }
        })
    }

    override fun onSplitTunnelingOptionClicked() {
        logger.info("Opening split tunnel settings activity..")
        connSettingsView.gotoSplitTunnelingSettings()
    }

    override fun saveKeepAlive(keepAlive: String) {
        interactor.getAppPreferenceInterface().keepAlive = keepAlive
    }

    override fun setKeepAlive(keepAlive: String) {
        interactor.getAppPreferenceInterface().keepAlive = keepAlive
    }

    override fun setPacketSize(size: String) {
        interactor.getAppPreferenceInterface().packetSize = size.toInt()
    }

    override fun setTheme(context: Context) {
        val savedThem = interactor.getAppPreferenceInterface().selectedTheme
        logger.debug("Setting theme to $savedThem")
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    private fun setUpAutoModePorts() {
        logger.debug("Setting auto mode ports.")
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                for (portMap in portMapResponse.portmap) {
                    if (portMap.protocol == PROTO_IKev2) {
                        interactor.getIKev2Port()
                    }
                    if (portMap.protocol == PROTO_UDP) {
                        interactor.getSavedUDPPort()
                    }
                    if (portMap.protocol == PROTO_TCP) {
                        interactor.getSavedTCPPort()
                    }
                    if (portMap.protocol == PROTO_STEALTH) {
                        interactor.getSavedSTEALTHPort()
                    }
                    if (portMap.protocol == PROTO_WS_TUNNEL) {
                        interactor.getSavedWSTunnelPort()
                    }
                    if (portMap.protocol == PROTO_WIRE_GUARD) {
                        interactor.getWireGuardPort()
                    }
                }
                val savedProtocol = interactor.getSavedProtocol()
                val savedConnectionMode = interactor.getSavedConnectionMode()
                connSettingsView.setKeepAliveContainerVisibility(
                        savedProtocol == PROTO_IKev2 && savedConnectionMode == CONNECTION_MODE_MANUAL
                )
                setUpKeepAlive()
            }
        })
    }

    fun setUpKeepAlive() {
        val isKeepAliveModeAuto =
                interactor.getAppPreferenceInterface().isKeepAliveModeAuto
        if (isKeepAliveModeAuto) {
            connSettingsView.setKeepAliveModeAdapter(
                    interactor.getResourceString(R.string.auto),
                    arrayOf(
                            interactor.getResourceString(R.string.auto),
                            interactor.getResourceString(R.string.manual)
                    )
            )
        } else {
            connSettingsView.setKeepAliveModeAdapter(
                    interactor.getResourceString(R.string.manual),
                    arrayOf(
                            interactor.getResourceString(R.string.auto),
                            interactor.getResourceString(R.string.manual)
                    )
            )
        }
        val keepAliveTime = interactor.getAppPreferenceInterface().keepAlive
        connSettingsView.setKeepAlive(keepAliveTime)
    }

    private fun setupLayoutBasedOnConnectionMode() {
        if (interactor.getSavedConnectionMode() == CONNECTION_MODE_AUTO) {
            connSettingsView.setupConnectionModeAdapter(
                    interactor.getResourceString(R.string.auto),
                    arrayOf(
                            interactor.getResourceString(R.string.auto),
                            interactor.getResourceString(R.string.manual)
                    )
            )
        } else {
            connSettingsView.setupConnectionModeAdapter(
                    interactor.getResourceString(R.string.manual),
                    arrayOf(
                            interactor.getResourceString(R.string.auto),
                            interactor.getResourceString(R.string.manual)
                    )
            )
        }
        setProtocolAdapter()
    }

    private fun setupPacketSizeMode() {
        val packetSizeModeAuto =
                interactor.getAppPreferenceInterface().isPackageSizeModeAuto
        if (packetSizeModeAuto) {
            connSettingsView.setupPacketSizeModeAdapter(
                    interactor.getResourceString(R.string.auto),
                    arrayOf(
                            interactor.getResourceString(R.string.auto),
                            interactor.getResourceString(R.string.manual)
                    )
            )
        } else {
            connSettingsView.setupPacketSizeModeAdapter(
                    interactor.getResourceString(R.string.manual),
                    arrayOf(
                            interactor.getResourceString(R.string.auto),
                            interactor.getResourceString(R.string.manual)
                    )
            )
        }
        val packetSize = interactor.getAppPreferenceInterface().packetSize
        connSettingsView.setPacketSize(packetSize.toString())
    }

    private fun setProtocolAdapter() {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                val savedProtocol = interactor.getSavedProtocol()
                var selectedPortMap: PortMap? = null
                val protocols: MutableList<String> = ArrayList()
                for (portMap in portMapResponse.portmap) {
                    if (portMap.protocol == savedProtocol) {
                        selectedPortMap = portMap
                    }
                    protocols.add(portMap.heading)
                }
                selectedPortMap = selectedPortMap ?: portMapResponse.portmap[0]
                if (selectedPortMap != null) {
                    connSettingsView.setupProtocolAdapter(
                            selectedPortMap.heading,
                            protocols.toTypedArray()
                    )
                    setPortMapAdapter(selectedPortMap.heading)
                }
            }
        })
    }// check network first

    // MTU detection experimental feature
    private val mtuSizeFromNetworkInterface: Unit
        get() {
            // check network first
            if (interactor.getVpnConnectionStateManager().isVPNConnected()) {
                connSettingsView.showToast(interactor.getResourceString(R.string.disconnect_from_vpn))
                return
            }
            val manager = appContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (manager.activeNetworkInfo == null || manager.activeNetworkInfo?.isConnected != true) {
                connSettingsView.showToast(interactor.getResourceString(R.string.no_network_detected))
                return
            }
            connSettingsView.packetSizeDetectionProgress(true)
            connSettingsView.setPacketSize(interactor.getResourceString(R.string.auto_detecting_packet_size))
            var prop: LinkProperties? = null
            val iFace: NetworkInterface
            val networks = manager.allNetworks
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                prop = manager.getLinkProperties(manager.activeNetwork)
            } else {
                for (network in networks) {
                    val networkInfo = manager.activeNetworkInfo
                    if (networkInfo?.isConnected == true) {
                        prop = manager.getLinkProperties(network)
                    }
                }
            }
            try {
                if (prop != null) {
                    iFace = NetworkInterface.getByName(prop.interfaceName)
                    currentPoint = iFace.mtu
                } else {
                    currentPoint = 1500
                }
                repeatPing()
            } catch (e: IOException) {
                e.printStackTrace()
                currentPoint = 1500
                repeatPing()
            }
        }

    private fun getProtocolFromHeading(portMapResponse: PortMapResponse, heading: String): String {
        for (map in portMapResponse.portmap) {
            if (map.heading == heading) {
                return map.protocol
            }
        }
        return PROTO_IKev2
    }

    private fun getSavedPort(protocol: String): String {
        return when (protocol) {
            PROTO_IKev2 -> interactor.getIKev2Port()
            PROTO_UDP -> interactor.getSavedUDPPort()
            PROTO_TCP -> interactor.getSavedTCPPort()
            PROTO_STEALTH -> interactor.getSavedSTEALTHPort()
            PROTO_WS_TUNNEL -> interactor.getSavedWSTunnelPort()
            PROTO_WIRE_GUARD -> interactor.getWireGuardPort()
            else -> "443"
        }
    }

    private fun isMtuSmallEnough(response: String): Boolean {
        return !response.contains("100% packet loss")
    }

    /**
     *
     */
    private val isPermissionProvided: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            (ContextCompat
                    .checkSelfPermission(
                            appContext,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED)
        } else {
            true
        }

    private fun ping(value: Int): String? {
        val size = value.toString()
        val runtime = Runtime.getRuntime()
        return try {
            val process = runtime
                    .exec("/system/bin/ping -c 2 -s $size -i 0.5 -W 3 -M do checkip.windscribe.com")
            val inputStream = process.inputStream
            if (inputStream != null) {
                IOUtils.toString(inputStream, StandardCharsets.UTF_8)
            } else {
                showMtuFailed()
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun repeatPing() {
        interactor.getCompositeDisposable()
                .add(
                        Observable.fromCallable { ping(currentPoint) }.subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(object : DisposableObserver<String>() {
                                    override fun onComplete() {
                                        dispose()
                                    }

                                    override fun onError(e: Throwable) {
                                        showMtuFailed()
                                        dispose()
                                    }

                                    override fun onNext(s: String) {
                                        if (isMtuSmallEnough(s)) {
                                            showMtuResult()
                                        } else {
                                            if (currentPoint > 10) {
                                                currentPoint -= 10
                                                repeatPing()
                                            }
                                        }
                                    }
                                })
                )
    }

    private fun setAutoStartMenu() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            connSettingsView.showAutoStartOnBoot()
        }
        if (interactor.getAppPreferenceInterface().autoStartOnBoot) {
            connSettingsView.setAutoStartOnBootToggle(R.drawable.ic_toggle_button_on)
        } else {
            interactor.getAppPreferenceInterface().autoStartOnBoot = false
            connSettingsView.setAutoStartOnBootToggle(R.drawable.ic_toggle_button_off)
        }
    }

    private fun setGpsSpoofingMenu() {
        connSettingsView.showGpsSpoofing()
        if (!isAppSelectedInMockLocationList(appContext.applicationContext)
                or !isDevModeOn(appContext) or permissionManager.isBackgroundPermissionGranted().not()
        ) {
            interactor.getAppPreferenceInterface().setGpsSpoofing(false)
        }
        // Gps spoofing
        if (interactor.getAppPreferenceInterface().isGpsSpoofingOn) {
            connSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_on)
        } else {
            connSettingsView.setGpsSpoofingToggle(R.drawable.ic_toggle_button_off)
        }
    }

    private fun setPortMapAdapter(heading: String) {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                val protocol = getProtocolFromHeading(portMapResponse, heading)
                val savedPort = getSavedPort(protocol)
                for (portMap in portMapResponse.portmap) {
                    if (portMap.protocol == protocol) {
                        connSettingsView.setupPortMapAdapter(savedPort, portMap.ports)
                    }
                }
                val savedProtocol = interactor.getSavedProtocol()
                val savedConnectionMode = interactor.getSavedConnectionMode()
                connSettingsView.setKeepAliveContainerVisibility(
                        savedProtocol == PROTO_IKev2 && savedConnectionMode == CONNECTION_MODE_MANUAL
                )
            }
        })
    }

    private fun showMtuFailed() {
        connSettingsView.setPacketSize("")
        connSettingsView.packetSizeDetectionProgress(false)
        connSettingsView.showToast(interactor.getResourceString(R.string.auto_package_size_detecting_failed))
        logger.info("Error getting optimal MTU size.")
    }

    private fun showMtuResult() {
        connSettingsView.setPacketSize(currentPoint.toString())
        interactor.getAppPreferenceInterface().packetSize = currentPoint
        connSettingsView.showToast(interactor.getResourceString(R.string.package_size_detected_successfully))
        connSettingsView.packetSizeDetectionProgress(false)
        currentPoint = 1500
    }

    private fun resetPotentialTrafficInfo() {
        val trafficVolume = interactor.getAppPreferenceInterface().fakeTrafficVolume
        if (trafficVolume === FakeTrafficVolume.Low) {
            connSettingsView.setPotentialTrafficUse(
                    String.format(
                            Locale.getDefault(),
                            "%dMB/Hour",
                            1737
                    )
            )
        } else if (trafficVolume === FakeTrafficVolume.Medium) {
            connSettingsView.setPotentialTrafficUse(
                    String.format(
                            Locale.getDefault(),
                            "%dMB/Hour",
                            6948
                    )
            )
        } else {
            connSettingsView.setPotentialTrafficUse(
                    String.format(
                            Locale.getDefault(),
                            "%dMB/Hour",
                            16572
                    )
            )
        }
    }

    override fun onNetworkOptionsClick() {
        permissionManager.withBackgroundLocationPermission { error ->
            if (error != null) {
                logger.debug(error)
            } else {
                connSettingsView.goToNetworkSecurity()
            }
        }
    }

    override fun onAntiCensorshipClick() {
        if (interactor.getAppPreferenceInterface().isAntiCensorshipOn) {
            connSettingsView.setAntiCensorshipToggle(R.drawable.ic_toggle_button_off)
            interactor.getAppPreferenceInterface().isAntiCensorshipOn = false
        } else {
            connSettingsView.setAntiCensorshipToggle(R.drawable.ic_toggle_button_on)
            interactor.getAppPreferenceInterface().isAntiCensorshipOn = true
        }
    }
}