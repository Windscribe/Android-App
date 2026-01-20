/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.windscribe.tv.adapter.InstalledAppsAdapter
import com.windscribe.tv.adapter.InstalledAppsData
import com.windscribe.tv.di.PerActivity
import com.windscribe.tv.settings.fragment.AccountFragment
import com.windscribe.tv.sort.SortByName
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.PortMapResponse.PortMap
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.LogRepository
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.BOOT_ALLOW
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.BOOT_BLOCK
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.CONNECTION_MODE_AUTO
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.CONNECTION_MODE_MANUAL
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DISABLED_MODE
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DNS_MODE_CUSTOM
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DNS_MODE_ROBERT
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.EXCLUSIVE_MODE
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.INCLUSIVE_MODE
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.LAN_ALLOW
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.LAN_BLOCK
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_STEALTH
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_TCP
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_UDP
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_WS_TUNNEL
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import javax.inject.Inject

@PerActivity
class SettingsPresenterImp @Inject constructor(
    private var settingView: SettingView,
    private val activityScope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val resourceHelper: ResourceHelper,
    private val apiCallManager: IApiCallManager,
    private val userRepository: UserRepository,
    private val localDbInterface: LocalDbInterface,
    private val autoConnectionManager: AutoConnectionManager,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val logRepository: LogRepository,
    private var proxyDNSManager: ProxyDNSManager,
    private val portMapRepository: com.windscribe.vpn.repository.PortMapRepository
) : SettingsPresenter, InstalledAppsAdapter.InstalledAppListener {
    private val installedAppList: MutableList<InstalledAppsData> = ArrayList()
    private var installedAppsAdapter: InstalledAppsAdapter? = null
    private val logger = LoggerFactory.getLogger("basic")
    override fun onDestroy() {
        logger.info("Disposing observer...")
        autoConnectionManager.reset()
    }

    override val isUserInGhostMode: Boolean
        get() = preferencesHelper.userIsInGhostMode()
    override val isUserPro: Boolean
        get() = (preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM)

    override fun onAddEmailClicked() {
        logger.info("Showing account in browser...")
        settingView.openEmailActivity(null)
    }

    override fun onAllowBootStartClick() {
        val allowStartOnBoot = preferencesHelper.autoStartOnBoot
        if (!allowStartOnBoot) {
            preferencesHelper.autoStartOnBoot = true
            settingView.setBootStartMode(BOOT_ALLOW)
        }
    }

    override fun onAllowLanClicked() {
        val allowByPassLanTraffic = preferencesHelper.lanByPass
        if (!allowByPassLanTraffic) {
            activityScope.launch {
                preferencesHelper.lanByPass = true
            }
            settingView.setLanTrafficMode(LAN_ALLOW)
        }
    }

    override fun onBlockBootStartClick() {
        val allowStartOnBoot = preferencesHelper.autoStartOnBoot
        if (allowStartOnBoot) {
            activityScope.launch {
                preferencesHelper.autoStartOnBoot = false
            }
            settingView.setBootStartMode(BOOT_BLOCK)
        }
    }

    override fun onBlockLanClicked() {
        val allowByPassLanTraffic = preferencesHelper.lanByPass
        if (allowByPassLanTraffic) {
            preferencesHelper.lanByPass = false
            settingView.setLanTrafficMode(LAN_BLOCK)
        }
    }

    override fun onCustomDNSClicked() {
        val dnsMode = preferencesHelper.dnsMode
        if (dnsMode != DNS_MODE_CUSTOM) {
            preferencesHelper.dnsMode = DNS_MODE_CUSTOM
            settingView.setCustomDNS(true)
            settingView.setCustomDNSAddressVisibility(true)
        }
    }

    override fun onRobertDNSClicked() {
        val dnsMode = preferencesHelper.dnsMode
        if (dnsMode != DNS_MODE_ROBERT) {
            preferencesHelper.dnsMode = DNS_MODE_ROBERT
            settingView.setCustomDNS(false)
            settingView.setCustomDNSAddressVisibility(false)
        }
    }

    override fun saveCustomDNSAddress(url: String) {
        val dnsAddress = preferencesHelper.dnsAddress
        if (dnsAddress != url && url.isNotEmpty()) {
            preferencesHelper.dnsAddress = url
            proxyDNSManager.invalidConfig = true
            activityScope.launch {
                if (!vpnConnectionStateManager.isVPNConnected()) {
                    proxyDNSManager.stopControlD()
                }
            }
        }
    }

    override fun onAllowAntiCensorshipClicked() {
        if (preferencesHelper.isAntiCensorshipOn.not()) {
            preferencesHelper.isAntiCensorshipOn = true
            settingView.setAntiCensorshipMode(true)
        }
    }

    override fun onBlockAntiCensorshipClicked() {
        if (preferencesHelper.isAntiCensorshipOn) {
            preferencesHelper.isAntiCensorshipOn = false
            settingView.setAntiCensorshipMode(false)
        }
    }

    override fun onConnectionModeAutoClicked() {
        if (CONNECTION_MODE_AUTO != (preferencesHelper.connectionMode ?: CONNECTION_MODE_AUTO)) {
            activityScope.launch {
                preferencesHelper.connectionMode = CONNECTION_MODE_AUTO
                preferencesHelper.savedProtocol = PROTO_IKev2
            }
            settingView.setupLayoutForAutoMode()
        }
    }

    override fun onConnectionModeManualClicked() {
        if (CONNECTION_MODE_MANUAL != (preferencesHelper.connectionMode ?: CONNECTION_MODE_AUTO)) {
            preferencesHelper.connectionMode = CONNECTION_MODE_MANUAL
            settingView.setupLayoutForManualMode()
            val savedProtocol = preferencesHelper.savedProtocol
            setProtocolAdapter(savedProtocol)
            setPortMapAdapter(savedProtocol)
        }
    }

    override fun onDisabledModeClick() {
        val splitRouting = preferencesHelper.splitTunnelToggle
        if (splitRouting) {
            activityScope.launch(Dispatchers.IO) {
                preferencesHelper.splitTunnelToggle = false
            }
            settingView.setSplitRouteMode(DISABLED_MODE)
        }
    }

    override fun onEmailResend() {
        settingView.openConfirmEmailActivity()
    }

    override fun onExclusiveModeClick() {
        val splitRouting = preferencesHelper.splitTunnelToggle
        val splitRoutingMode = preferencesHelper.splitRoutingMode
        if ((splitRoutingMode != EXCLUSIVE_MODE) or !splitRouting) {
            activityScope.launch(Dispatchers.IO) {
                preferencesHelper.splitTunnelToggle = true
                preferencesHelper.splitRoutingMode = EXCLUSIVE_MODE
            }
            settingView.setSplitRouteMode(EXCLUSIVE_MODE)
            addWindScribeToList(false)
        }
    }

    override fun onInclusiveModeClick() {
        val splitRouting = preferencesHelper.splitTunnelToggle
        val splitRoutingMode = preferencesHelper.splitRoutingMode
        if ((splitRoutingMode != INCLUSIVE_MODE) or !splitRouting) {
            activityScope.launch(Dispatchers.IO) {
                preferencesHelper.splitTunnelToggle = true
                preferencesHelper.splitRoutingMode = INCLUSIVE_MODE
            }
            settingView.setSplitRouteMode(INCLUSIVE_MODE)
            addWindScribeToList(true)
        }
    }

    override fun onInstalledAppClick(updatedModel: InstalledAppsData?, reloadAdapter: Boolean) {
        activityScope.launch(Dispatchers.IO) {
            try {
                val apps = preferencesHelper.installedApps
                withContext(Dispatchers.Main) {
                    updatedModel?.let { saveApps(apps, it, reloadAdapter) }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    val list: MutableList<String> = ArrayList()
                    updatedModel?.let { saveApps(list, it, reloadAdapter) }
                }
            }
        }
    }

    override fun onLanguageSelected(selectedLanguage: String) {
        // Save the selected language
        val savedLanguage = preferencesHelper.savedLanguage
        if (savedLanguage == selectedLanguage) {
            logger.info("Language selected is same as saved. No action taken...")
        } else {
            preferencesHelper.savedLanguage = selectedLanguage
            settingView.reloadApp()
        }
    }

    override fun onLoginAndClaimClick() {
        val ghostMode = preferencesHelper.userIsInGhostMode()
        val proUser = (preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM)
        if (!proUser && ghostMode) {
            settingView.goToLogin()
        } else if (ghostMode) {
            settingView.goToClaimAccount()
        }
    }

    override fun onPortSelected(protocol: String, port: String) {
        logger.info("Saving selected port...")
        portMapRepository.getPortMapWithCallback { portMapResponse ->
            when (getProtocolFromHeading(portMapResponse, protocol)) {
                PROTO_IKev2 -> {
                    logger.info("Saving selected IKev2 port...")
                    preferencesHelper.iKEv2Port = port
                }

                PROTO_UDP -> {
                    logger.info("Saving selected udp port...")
                    preferencesHelper.savedUDPPort = port
                }

                PROTO_TCP -> {
                    logger.info("Saving selected tcp port...")
                    preferencesHelper.savedTCPPort = port
                }

                PROTO_STEALTH -> {
                    logger.info("Saving selected stealth port...")
                    preferencesHelper.savedSTEALTHPort = port
                }

                PROTO_WS_TUNNEL -> {
                    logger.info("Saving selected ws port...")
                    preferencesHelper.savedWSTunnelPort = port
                }

                PROTO_WIRE_GUARD -> {
                    logger.info("Saving selected wire guard port...")
                    preferencesHelper.wireGuardPort = port
                }

                else -> {
                    logger.info("Saving default port (udp)...")
                    preferencesHelper.savedUDPPort = port
                }
            }
        }
    }

    override fun onProtocolSelected(protocol: String) {
        logger.debug("Saving selected protocol.")
        portMapRepository.getPortMapWithCallback { portMapResponse ->
            val protocolFromHeading = getProtocolFromHeading(portMapResponse, protocol)
            val savedProtocol = preferencesHelper.savedProtocol
            if (savedProtocol == protocolFromHeading) {
                logger.debug("Protocol re-selected is same as saved. No action taken...")
            } else {
                logger.info("Saving selected protocol...")
                preferencesHelper.savedProtocol = protocolFromHeading
                setPortMapAdapter(protocol)
            }
        }
    }

    override fun onSendDebugClicked() {
        logger.info("Preparing debug file...")
        settingView.showProgress(settingView.getResourceString(com.windscribe.vpn.R.string.sending_debug_log))
        activityScope.launch(Dispatchers.IO) {
            try {
                val result = logRepository.onSendLog()
                when (result) {
                    is CallResult.Success -> {
                        withContext(Dispatchers.Main) {
                            settingView.hideProgress()
                            settingView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.app_log_submitted))
                        }
                    }

                    is CallResult.Error -> {
                        withContext(Dispatchers.Main) {
                            settingView.hideProgress()
                            logger.debug("Error Submitting Log from Api: " + result.errorMessage)
                            settingView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.log_submission_failed))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    settingView.hideProgress()
                    logger.debug(
                        "Error Submitting Log: " + WindError.instance.convertThrowableToString(
                            e
                        )
                    )
                    settingView.showToast(
                        resourceHelper.getString(com.windscribe.vpn.R.string.log_submission_failed)
                    )
                }
            }
        }
    }

    override fun onSignOutClicked() {
        activityScope.launch { userRepository.logout() }
    }

    override fun onSortSelected(newSort: String) {
        preferencesHelper.selection = newSort
        activityScope.launch(Dispatchers.IO) {
            try {
                val serverStatusUpdateTable =
                    localDbInterface.getServerStatus(preferencesHelper.userName)
                localDbInterface.insertOrUpdateStatus(
                    ServerStatusUpdateTable(
                        preferencesHelper.userName,
                        if (serverStatusUpdateTable.serverStatus == 0) 1 else 0
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onUpgradeClicked(textViewText: String) {
        if (resourceHelper.getString(com.windscribe.vpn.R.string.upgrade_case_normal) == textViewText) {
            // User is free user.. goto upgrade activity
            logger.info("Showing upgrade dialog to the user...")
            settingView.openUpgradeActivity()
        } else {
            // User is a pro user... no actions taken
            logger.info("User is already pro no actions taken...")
        }
    }

    override fun observeUserData(settingsActivity: SettingActivity) {
        userRepository.user.observe(settingsActivity, this::setAccountInfo)
    }

    private fun setAccountInfo(user: User) {
        settingView.hideProgress()
        // Username
        settingView.setUsername(if (user.isGhost) "" else user.userName)
        // Email
        when (user.emailStatus) {
            User.EmailStatus.NoEmail -> {
                settingView.setEmailState(
                    if (user.isPro) AccountFragment.Status.NOT_ADDED_PRO else AccountFragment.Status.NOT_ADDED,
                    null
                )
            }

            User.EmailStatus.EmailProvided -> {
                settingView.setEmailState(AccountFragment.Status.NOT_CONFIRMED, user.email)
            }

            User.EmailStatus.Confirmed -> {
                settingView.setEmailState(AccountFragment.Status.CONFIRMED, user.email)
            }
        }
        user.email?.let { settingView.setEmail(it) }

        // Plan
        if (user.isPro) {
            setExpiryOrResetDate(true, user.expiryDate)
            settingView.setPlanName(
                resourceHelper.getString(com.windscribe.vpn.R.string.unlimited_data)
            )
            settingView.setupLayoutForPremiumUser(
                resourceHelper.getString(com.windscribe.vpn.R.string.plan_pro)
            )
        } else if (user.isAlaCarteUnlimitedPlan) {
            setExpiryOrResetDate(true, user.expiryDate)
            settingView.setPlanName(
                resourceHelper.getString(com.windscribe.vpn.R.string.unlimited_data)
            )
            settingView.setupLayoutForPremiumUser(
                resourceHelper.getString(com.windscribe.vpn.R.string.a_la_carte_unlimited_plan)
            )
        } else {
            settingView.setupLayoutForFreeUser(
                resourceHelper.getString(com.windscribe.vpn.R.string.upgrade_case_normal)
            )
            setExpiryOrResetDate(false, user.resetDate)
            if (user.maxData == -1L) {
                settingView.setPlanName(
                    resourceHelper.getString(com.windscribe.vpn.R.string.unlimited_data)
                )
            } else {
                val maxTrafficData: Long = user.maxData / UserStatusConstants.GB_DATA
                settingView.setPlanName(
                    maxTrafficData.toString() + resourceHelper.getString(com.windscribe.vpn.R.string.gb_per_month)
                )
            }
        }
    }

    override fun setUpTabMenu() {
        val ghostMode = preferencesHelper.userIsInGhostMode()
        val proUser = (preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM)
        if (!proUser && ghostMode) {
            settingView.setUpTabLayoutForGhostMode()
        } else if (ghostMode) {
            settingView.setUpTabLayoutForGhostModePro()
        } else {
            settingView.setUpTabLayoutForLoggedInUser()
        }
    }

    override fun setupLayoutBasedOnConnectionMode() {
        val savedConnectionMode = preferencesHelper.connectionMode ?: CONNECTION_MODE_AUTO
        if (CONNECTION_MODE_MANUAL == savedConnectionMode) {
            settingView.setupLayoutForManualMode()
            logger.info(
                "Saved connection mode is " + savedConnectionMode + ". No need to change layout settings." + " Continue with manual mode as default"
            )
            val savedProtocol = preferencesHelper.savedProtocol
            setProtocolAdapter(savedProtocol)
        } else {
            logger.info("Saved connection mode is $savedConnectionMode")
            settingView.setupLayoutForAutoMode()
        }
        val splitRouting = preferencesHelper.splitTunnelToggle
        val splitRoutingMode = preferencesHelper.splitRoutingMode
        settingView.setSplitRouteMode(if (splitRouting) splitRoutingMode else DISABLED_MODE)
        val allowLanTraffic = preferencesHelper.lanByPass
        settingView.setLanTrafficMode(if (allowLanTraffic) LAN_ALLOW else LAN_BLOCK)
        val allowBootStart = preferencesHelper.autoStartOnBoot
        settingView.setBootStartMode(if (allowBootStart) BOOT_ALLOW else BOOT_BLOCK)
        settingView.setAntiCensorshipMode(preferencesHelper.isAntiCensorshipOn)
        settingView.setCustomDNS(preferencesHelper.dnsMode == DNS_MODE_CUSTOM)
        settingView.setCustomDNSAddress(preferencesHelper.dnsAddress ?: "")
        settingView.setCustomDNSAddressVisibility(preferencesHelper.dnsMode == DNS_MODE_CUSTOM)
        setupAppListAdapter()
    }

    override fun setupLayoutForDebugTab() {
        settingView.setDebugLogProgress(
            resourceHelper.getString(com.windscribe.vpn.R.string.loading), ""
        )
        activityScope.launch(Dispatchers.IO) {
            try {
                val log = logRepository.getPartialLog()
                withContext(Dispatchers.Main) {
                    settingView.setDebugLog(log)
                    settingView.setDebugLogProgress("", "")
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    settingView.setDebugLogProgress("", "Error loading logs")
                }
            }
        }
    }

    override fun setupLayoutForGeneralTab() {
        val savedLanguage = preferencesHelper.savedLanguage
        // Setup language settings
        settingView.setupLanguageAdapter(
            savedLanguage, resourceHelper.getStringArray(com.windscribe.vpn.R.array.language)
        )
        val savedSort = preferencesHelper.selection
        settingView.setupSortAdapter(
            resourceHelper.getStringArray(com.windscribe.vpn.R.array.order_list),
            savedSort,
            resourceHelper.getStringArray(com.windscribe.vpn.R.array.order_list_keys)
        )
    }

    override fun showLayoutBasedOnUserType() {
        val mUserStatus = preferencesHelper.userStatus
        logger.info("Showing layout based on current user status...[status]: $mUserStatus")
        if (mUserStatus == UserStatusConstants.USER_STATUS_PREMIUM) {
            settingView.setupLayoutForPremiumUser(resourceHelper.getString(com.windscribe.vpn.R.string.plan_pro))
        } else {
            // Get User Session Data... if not present call and save session
            settingView.setupLayoutForFreeUser(resourceHelper.getString(com.windscribe.vpn.R.string.upgrade_case_normal))
        }
    }

    override fun updateUserDataFromApi() {
        activityScope.launch(Dispatchers.IO) {
            val result = result<UserSessionResponse> {
                apiCallManager.getSessionGeneric(null)
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is CallResult.Success -> {
                        userRepository.reload(result.data)
                    }

                    is CallResult.Error -> {
                        logger.debug("Error while making get session call:" + result.errorMessage)
                    }
                }
            }
        }
    }

    private fun addWindScribeToList(checked: Boolean) {
        val pm = appContext.packageManager
        val packageName = appContext.packageName
        try {
            val applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val mData = InstalledAppsData(
                pm.getApplicationLabel(applicationInfo).toString(),
                applicationInfo.packageName,
                pm.getApplicationIcon(applicationInfo),
                false
            )
            mData.isChecked = checked
            onInstalledAppClick(mData, true)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun getProtocolFromHeading(portMapResponse: PortMapResponse?, heading: String): String {
        portMapResponse?.let {
            for (map in it.portmap) {
                if (map.heading == heading) {
                    return map.protocol
                }
            }
        }
        return PROTO_IKev2
    }

    private fun getSavedPort(protocol: String?): String {
        return when (protocol) {
            PROTO_IKev2 -> preferencesHelper.iKEv2Port
            PROTO_UDP -> preferencesHelper.savedUDPPort
            PROTO_TCP -> preferencesHelper.savedTCPPort
            PROTO_STEALTH -> preferencesHelper.savedSTEALTHPort
            PROTO_WS_TUNNEL -> preferencesHelper.savedWSTunnelPort
            PROTO_WIRE_GUARD -> preferencesHelper.wireGuardPort
            else -> "443"
        }
    }

    private fun modifyList(savedApps: List<String>) {
        val pm = appContext.packageManager
        activityScope.launch(Dispatchers.IO) {
            try {
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                installedAppList.clear()
                for (applicationInfo in packages) {
                    val isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1
                    val mData = InstalledAppsData(
                        pm.getApplicationLabel(applicationInfo).toString(),
                        applicationInfo.packageName,
                        pm.getApplicationIcon(applicationInfo),
                        isSystemApp
                    )
                    for (installedAppsData in savedApps) {
                        if (mData.packageName == installedAppsData) {
                            mData.isChecked = true
                        }
                    }
                    installedAppList.add(mData)
                }
                Collections.sort(installedAppList, SortByName())
                withContext(Dispatchers.Main) {
                    settingView.hideProgress()
                    installedAppsAdapter = InstalledAppsAdapter(
                        installedAppList, this@SettingsPresenterImp
                    )
                    installedAppsAdapter?.let {
                        settingView.setupAppsAdapter(it)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    settingView.hideProgress()
                }
            }
        }
    }

    private fun saveApps(
        savedList: List<String>, updatedApp: InstalledAppsData, reloadAdapter: Boolean
    ) {
        val list = savedList.toMutableList()
        if (updatedApp.isChecked) {
            list.add(updatedApp.packageName)
        } else {
            list.remove(updatedApp.packageName)
        }
        activityScope.launch(Dispatchers.IO) {
            preferencesHelper.installedApps = list
        }
        if (reloadAdapter) {
            installedAppsAdapter?.updateApp(updatedApp.packageName, updatedApp.isChecked)
        }
    }

    private fun setExpiryOrResetDate(premiumUser: Boolean, resetOrExpiryDate: String?) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        if (resetOrExpiryDate != null) {
            try {
                val lastResetDate = formatter.parse(resetOrExpiryDate)
                val c = Calendar.getInstance()
                lastResetDate?.let {
                    c.time = it
                    if (!premiumUser) {
                        c.add(Calendar.DATE, 30)
                        val nextResetDate = c.time
                        settingView.setResetDate(
                            resourceHelper.getString(com.windscribe.vpn.R.string.reset_date),
                            formatter.format(nextResetDate)
                        )
                    } else {
                        val nextResetDate = c.time
                        settingView.setResetDate(
                            resourceHelper.getString(com.windscribe.vpn.R.string.expiry_date),
                            formatter.format(nextResetDate)
                        )
                    }
                }
            } catch (e: ParseException) {
                logger.debug(
                    "Could not parse date data. " + WindError.instance.convertErrorToString(
                        e
                    )
                )
            }
        }
    }

    private fun setPortMapAdapter(heading: String) {
        portMapRepository.getPortMapWithCallback { portMapResponse ->
            val protocol = getProtocolFromHeading(portMapResponse, heading)
            val savedPort = getSavedPort(protocol)
            for (portMap in portMapResponse.portmap) {
                if (portMap.protocol == protocol) {
                    settingView.setupPortMapAdapter(savedPort, portMap.ports)
                }
            }
        }
    }

    private fun setProtocolAdapter(savedProtocol: String) {
        portMapRepository.getPortMapWithCallback { portMapResponse ->
            var selectedPortMap: PortMap? = null
            val protocols: MutableList<String> = ArrayList()
            for (portMap in portMapResponse.portmap) {
                if (portMap.protocol == savedProtocol) {
                    selectedPortMap = portMap
                }
                protocols.add(portMap.heading)
            }
            selectedPortMap = selectedPortMap ?: portMapResponse.portmap[0]
            selectedPortMap?.let {
                settingView.setupProtocolAdapter(it.heading, protocols)
                setPortMapAdapter(it.heading)
            }
        }
    }

    private fun setupAppListAdapter() {
        settingView.showProgress(resourceHelper.getString(com.windscribe.vpn.R.string.loading))
        activityScope.launch(Dispatchers.IO) {
            val apps = preferencesHelper.installedApps
            withContext(Dispatchers.Main) {
                settingView.hideProgress()
                modifyList(apps)
            }
        }
    }
}
