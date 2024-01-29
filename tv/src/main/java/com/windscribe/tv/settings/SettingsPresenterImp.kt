/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.windscribe.tv.R
import com.windscribe.tv.adapter.InstalledAppsAdapter
import com.windscribe.tv.adapter.InstalledAppsData
import com.windscribe.tv.di.PerActivity
import com.windscribe.tv.settings.fragment.AccountFragment
import com.windscribe.tv.sort.SortByName
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.ActivityInteractorImpl.PortMapLoadCallback
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.Windscribe.Companion.getExecutorService
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.PortMapResponse.PortMap
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants.BOOT_ALLOW
import com.windscribe.vpn.constants.PreferencesKeyConstants.BOOT_BLOCK
import com.windscribe.vpn.constants.PreferencesKeyConstants.CONNECTION_MODE_AUTO
import com.windscribe.vpn.constants.PreferencesKeyConstants.CONNECTION_MODE_MANUAL
import com.windscribe.vpn.constants.PreferencesKeyConstants.DISABLED_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.EXCLUSIVE_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.INCLUSIVE_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.LAN_ALLOW
import com.windscribe.vpn.constants.PreferencesKeyConstants.LAN_BLOCK
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_STEALTH
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_TCP
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_UDP
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WS_TUNNEL
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.SessionErrorHandler
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable
import com.windscribe.vpn.model.User
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@PerActivity
class SettingsPresenterImp @Inject constructor(
        private var settingView: SettingView,
        private var interactor: ActivityInteractor
) : SettingsPresenter, InstalledAppsAdapter.InstalledAppListener {
    private val installedAppList: MutableList<InstalledAppsData> = ArrayList()
    private var installedAppsAdapter: InstalledAppsAdapter? = null
    private val logger = LoggerFactory.getLogger("setting_presenter")
    override fun onDestroy() {
        logger.info("Disposing observer...")
        interactor.getAutoConnectionManager().reset()
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
    }

    override val isUserInGhostMode: Boolean
        get() = interactor.getAppPreferenceInterface().userIsInGhostMode()
    override val isUserPro: Boolean
        get() = (
                interactor.getAppPreferenceInterface().userStatus
                        == UserStatusConstants.USER_STATUS_PREMIUM
                )

    override fun onAddEmailClicked() {
        logger.info("Showing account in browser...")
        settingView.openEmailActivity(null)
    }

    override fun onAllowBootStartClick() {
        val allowStartOnBoot = interactor.getAppPreferenceInterface().autoStartOnBoot
        if (!allowStartOnBoot) {
            interactor.getAppPreferenceInterface().autoStartOnBoot = true
            settingView.setBootStartMode(BOOT_ALLOW)
        }
    }

    override fun onAllowLanClicked() {
        val allowByPassLanTraffic = interactor.getAppPreferenceInterface().lanByPass
        if (!allowByPassLanTraffic) {
            getExecutorService()
                    .submit { interactor.getAppPreferenceInterface().lanByPass = true }
            settingView.setLanTrafficMode(LAN_ALLOW)
        }
    }

    override fun onBlockBootStartClick() {
        val allowStartOnBoot = interactor.getAppPreferenceInterface().autoStartOnBoot
        if (allowStartOnBoot) {
            getExecutorService()
                    .submit { interactor.getAppPreferenceInterface().autoStartOnBoot = false }
            settingView.setBootStartMode(BOOT_BLOCK)
        }
    }

    override fun onBlockLanClicked() {
        val allowByPassLanTraffic = interactor.getAppPreferenceInterface().lanByPass
        if (allowByPassLanTraffic) {
            interactor.getAppPreferenceInterface().lanByPass = false
            settingView.setLanTrafficMode(LAN_BLOCK)
        }
    }

    override fun onAllowAntiCensorshipClicked() {
        if (interactor.getAppPreferenceInterface().isAntiCensorshipOn.not()) {
            interactor.getAppPreferenceInterface().isAntiCensorshipOn = true
            settingView.setAntiCensorshipMode(true)
        }
    }

    override fun onBlockAntiCensorshipClicked() {
        if (interactor.getAppPreferenceInterface().isAntiCensorshipOn) {
            interactor.getAppPreferenceInterface().isAntiCensorshipOn = false
            settingView.setAntiCensorshipMode(false)
        }
    }

    override fun onConnectionModeAutoClicked() {
        if (CONNECTION_MODE_AUTO != interactor.getSavedConnectionMode()) {
            getExecutorService().submit {
                interactor.saveConnectionMode(CONNECTION_MODE_AUTO)
                interactor.getAppPreferenceInterface().nextProtocol(null)
                interactor.saveProtocol(PROTO_IKev2)
            }
            settingView.setupLayoutForAutoMode()
        }
    }

    override fun onConnectionModeManualClicked() {
        if (CONNECTION_MODE_MANUAL != interactor.getSavedConnectionMode()) {
            interactor.saveConnectionMode(CONNECTION_MODE_MANUAL)
            settingView.setupLayoutForManualMode()
            val savedProtocol = interactor.getSavedProtocol()
            setProtocolAdapter(savedProtocol)
            setPortMapAdapter(savedProtocol)
        }
    }

    override fun onDisabledModeClick() {
        val splitRouting = interactor.getAppPreferenceInterface().splitTunnelToggle
        if (splitRouting) {
            getExecutorService()
                    .submit {
                        interactor.getAppPreferenceInterface().splitTunnelToggle = false
                    }
            settingView.setSplitRouteMode(DISABLED_MODE)
        }
    }

    override fun onEmailResend() {
        settingView.openConfirmEmailActivity()
    }

    override fun onExclusiveModeClick() {
        val splitRouting = interactor.getAppPreferenceInterface().splitTunnelToggle
        val splitRoutingMode = interactor.getAppPreferenceInterface().splitRoutingMode
        if ((splitRoutingMode != EXCLUSIVE_MODE) or !splitRouting) {
            getExecutorService().submit {
                interactor.getAppPreferenceInterface().splitTunnelToggle = true
                interactor.getAppPreferenceInterface().saveSplitRoutingMode(EXCLUSIVE_MODE)
            }
            settingView.setSplitRouteMode(EXCLUSIVE_MODE)
            addWindScribeToList(false)
        }
    }

    override fun onInclusiveModeClick() {
        val splitRouting = interactor.getAppPreferenceInterface().splitTunnelToggle
        val splitRoutingMode = interactor.getAppPreferenceInterface().splitRoutingMode
        if ((splitRoutingMode != INCLUSIVE_MODE) or !splitRouting) {
            getExecutorService().submit {
                interactor.getAppPreferenceInterface().splitTunnelToggle = true
                interactor.getAppPreferenceInterface().saveSplitRoutingMode(INCLUSIVE_MODE)
            }
            settingView.setSplitRouteMode(INCLUSIVE_MODE)
            addWindScribeToList(true)
        }
    }

    override fun onInstalledAppClick(updatedModel: InstalledAppsData?, reloadAdapter: Boolean) {
        interactor.getCompositeDisposable().add(
                interactor.getAppPreferenceInterface().installedApps
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(object : DisposableSingleObserver<List<String>>() {
                            override fun onError(e: Throwable) {
                                val list: MutableList<String> = ArrayList()
                                updatedModel?.let { saveApps(list, it, reloadAdapter) }
                            }

                            override fun onSuccess(installedAppsData: List<String>) {
                                updatedModel?.let { saveApps(installedAppsData, it, reloadAdapter) }
                            }
                        })
        )
    }

    override fun onLanguageSelected(selectedLanguage: String) {
        // Save the selected language
        val savedLanguage = interactor.getSavedLanguage()
        if (savedLanguage == selectedLanguage) {
            logger.info("Language selected is same as saved. No action taken...")
        } else {
            interactor.saveSelectedLanguage(selectedLanguage)
            settingView.reloadApp()
        }
    }

    override fun onLoginAndClaimClick() {
        val ghostMode = interactor.getAppPreferenceInterface().userIsInGhostMode()
        val proUser = (
                interactor.getAppPreferenceInterface().userStatus
                        == UserStatusConstants.USER_STATUS_PREMIUM
                )
        if (!proUser && ghostMode) {
            settingView.goToLogin()
        } else if (ghostMode) {
            settingView.goToClaimAccount()
        }
    }

    override fun onPortSelected(protocol: String, port: String) {
        logger.info("Saving selected port...")
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                when (getProtocolFromHeading(portMapResponse, protocol)) {
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
                        logger.info("Saving selected ws port...")
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
            }
        })
    }

    override fun onProtocolSelected(protocol: String) {
        logger.debug("Saving selected protocol.")
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                val protocolFromHeading = getProtocolFromHeading(portMapResponse, protocol)
                val savedProtocol = interactor.getSavedProtocol()
                if (savedProtocol == protocolFromHeading) {
                    logger.debug("Protocol re-selected is same as saved. No action taken...")
                } else {
                    logger.info("Saving selected protocol...")
                    interactor.saveProtocol(protocolFromHeading)
                    setPortMapAdapter(protocol)
                }
            }
        })
    }

    override fun onSendDebugClicked() {
        logger.info("Preparing debug file...")
        settingView.showProgress(settingView.getResourceString(R.string.sending_debug_log))
        val logMap: MutableMap<String, String> = HashMap()
        logMap[UserStatusConstants.CURRENT_USER_NAME] =
                interactor.getAppPreferenceInterface().userName
        interactor.getCompositeDisposable().add(
                Single.fromCallable { interactor.getEncodedLog() }
                        .flatMap { encodedLog: String ->
                            logger.info("Reading log file successful, submitting app log...")
                            logMap[NetworkKeyConstants.POST_LOG_FILE_KEY] = encodedLog
                            interactor.getApiCallManager().postDebugLog(logMap)
                        }.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(
                                object :
                                        DisposableSingleObserver<GenericResponseClass<GenericSuccess?, ApiErrorResponse?>?>() {
                                    override fun onError(e: Throwable) {
                                        logger.debug(
                                                "Error Submitting Log: " +
                                                        WindError.instance.convertThrowableToString(e)
                                        )
                                        settingView.showToast(
                                                interactor.getResourceString(R.string.log_submission_failed)
                                        )
                                        settingView.hideProgress()
                                    }

                                    override fun onSuccess(
                                            appLogSubmissionResponse: GenericResponseClass<GenericSuccess?, ApiErrorResponse?>
                                    ) {
                                        settingView.hideProgress()
                                        if (appLogSubmissionResponse.dataClass?.isSuccessful == true) {
                                            settingView.showToast(
                                                    interactor.getResourceString(R.string.app_log_submitted)
                                            )
                                        } else if (appLogSubmissionResponse.errorClass != null) {
                                            appLogSubmissionResponse.errorClass?.let {
                                                settingView.showToast(
                                                        SessionErrorHandler.instance.getErrorMessage(
                                                                it
                                                        )
                                                )
                                            }
                                        } else {
                                            settingView.showToast(
                                                    interactor.getResourceString(R.string.log_submission_failed)
                                            )
                                        }
                                    }
                                })
        )
    }

    override fun onSignOutClicked() {
        interactor.getMainScope().launch { interactor.getUserRepository().logout() }
    }

    override fun onSortSelected(newSort: String) {
        interactor.getAppPreferenceInterface().saveSelection(newSort)
        interactor.getCompositeDisposable().add(
                interactor.getServerStatus()
                        .flatMapCompletable(
                                Function { serverStatusUpdateTable: ServerStatusUpdateTable ->
                                    interactor
                                            .updateServerList(if (serverStatusUpdateTable.serverStatus == 0) 1 else 0)
                                } as Function<ServerStatusUpdateTable, Completable>
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(object : DisposableCompletableObserver() {
                            override fun onComplete() {}
                            override fun onError(e: Throwable) {}
                        })
        )
    }

    override fun onUpgradeClicked(textViewText: String) {
        if (interactor.getResourceString(R.string.upgrade_case_normal) == textViewText) {
            // User is free user.. goto upgrade activity
            logger.info("Showing upgrade dialog to the user...")
            settingView.openUpgradeActivity()
        } else {
            // User is a pro user... no actions taken
            logger.info("User is already pro no actions taken...")
        }
    }

    override fun observeUserData(settingsActivity: SettingActivity) {
        interactor.getUserRepository().user.observe(settingsActivity, this::setAccountInfo)
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
                    interactor.getResourceString(R.string.unlimited_data)
            )
            settingView.setupLayoutForPremiumUser(
                    interactor.getResourceString(R.string.plan_pro)
            )
        } else if (user.isAlaCarteUnlimitedPlan) {
            setExpiryOrResetDate(true, user.expiryDate)
            settingView.setPlanName(
                    interactor.getResourceString(R.string.unlimited_data)
            )
            settingView.setupLayoutForPremiumUser(
                    interactor.getResourceString(R.string.a_la_carte_unlimited_plan)
            )
        } else {
            settingView.setupLayoutForFreeUser(
                    interactor.getResourceString(R.string.upgrade_case_normal)
            )
            setExpiryOrResetDate(false, user.resetDate)
            if (user.maxData == -1L) {
                settingView.setPlanName(
                        interactor.getResourceString(R.string.unlimited_data)
                )
            } else {
                val maxTrafficData: Long = user.maxData / UserStatusConstants.GB_DATA
                settingView.setPlanName(
                        maxTrafficData.toString() + interactor.getResourceString(R.string.gb_per_month)
                )
            }
        }
    }

    override fun setUpTabMenu() {
        val ghostMode = interactor.getAppPreferenceInterface().userIsInGhostMode()
        val proUser = (
                interactor.getAppPreferenceInterface().userStatus
                        == UserStatusConstants.USER_STATUS_PREMIUM
                )
        if (!proUser && ghostMode) {
            settingView.setUpTabLayoutForGhostMode()
        } else if (ghostMode) {
            settingView.setUpTabLayoutForGhostModePro()
        } else {
            settingView.setUpTabLayoutForLoggedInUser()
        }
    }

    override fun setupLayoutBasedOnConnectionMode() {
        val savedConnectionMode = interactor.getSavedConnectionMode()
        if (CONNECTION_MODE_MANUAL == savedConnectionMode) {
            settingView.setupLayoutForManualMode()
            logger
                    .info(
                            "Saved connection mode is " + savedConnectionMode + ". No need to change layout settings." +
                                    " Continue with manual mode as default"
                    )
            val savedProtocol = interactor.getSavedProtocol()
            setProtocolAdapter(savedProtocol)
        } else {
            logger.info("Saved connection mode is $savedConnectionMode")
            settingView.setupLayoutForAutoMode()
        }
        val splitRouting = interactor.getAppPreferenceInterface().splitTunnelToggle
        val splitRoutingMode = interactor.getAppPreferenceInterface().splitRoutingMode
        settingView.setSplitRouteMode(if (splitRouting) splitRoutingMode else DISABLED_MODE)
        val allowLanTraffic = interactor.getAppPreferenceInterface().lanByPass
        settingView.setLanTrafficMode(if (allowLanTraffic) LAN_ALLOW else LAN_BLOCK)
        val allowBootStart = interactor.getAppPreferenceInterface().autoStartOnBoot
        settingView.setBootStartMode(if (allowBootStart) BOOT_ALLOW else BOOT_BLOCK)
        settingView.setAntiCensorshipMode(interactor.getAppPreferenceInterface().isAntiCensorshipOn)
        setupAppListAdapter()
    }

    override fun setupLayoutForDebugTab() {
        settingView.setDebugLogProgress(interactor.getResourceString(R.string.loading), "")
        interactor.getCompositeDisposable()
                .add(Single.fromCallable { interactor.getPartialLog() }.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object : DisposableSingleObserver<List<String>>() {
                            override fun onError(e: Throwable) {
                                settingView.setDebugLogProgress("", "Error loading logs")
                            }

                            override fun onSuccess(s: List<String>) {
                                settingView.setDebugLog(s)
                                settingView.setDebugLogProgress("", "")
                            }
                        })
                )
    }

    override fun setupLayoutForGeneralTab() {
        val savedLanguage = interactor.getSavedLanguage()
        // Setup language settings
        settingView.setupLanguageAdapter(savedLanguage, interactor.getLanguageList())
        val savedSort = interactor.getAppPreferenceInterface().selection
        settingView.setupSortAdapter(
                interactor.getStringArray(R.array.order_list),
                savedSort,
                interactor.getStringArray(R.array.order_list_keys)
        )
    }

    override fun showLayoutBasedOnUserType() {
        val mUserStatus = interactor.getAppPreferenceInterface().userStatus
        logger.info("Showing layout based on current user status...[status]: $mUserStatus")
        if (mUserStatus == UserStatusConstants.USER_STATUS_PREMIUM) {
            settingView.setupLayoutForPremiumUser(interactor.getResourceString(R.string.plan_pro))
        } else {
            // Get User Session Data... if not present call and save session
            settingView.setupLayoutForFreeUser(interactor.getResourceString(R.string.upgrade_case_normal))
        }
    }

    override fun updateUserDataFromApi() {
        interactor.getCompositeDisposable().add(
                interactor.getApiCallManager()
                        .getSessionGeneric(null)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(
                                object :
                                        DisposableSingleObserver<GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>?>() {
                                    override fun onError(e: Throwable) {
                                        // Error in API Call
                                        logger.debug(
                                                "Error while making get session call:" +
                                                        WindError.instance.convertThrowableToString(e)
                                        )
                                    }

                                    override fun onSuccess(
                                            userSessionResponse: GenericResponseClass<UserSessionResponse?, ApiErrorResponse?>
                                    ) {
                                        if (userSessionResponse.dataClass != null) {
                                            interactor.getUserRepository().reload(userSessionResponse.dataClass)
                                        } else if (userSessionResponse.errorClass != null) {
                                            // Server responded with error!
                                            logger.debug(
                                                    "Server returned error during get session call." +
                                                            userSessionResponse.errorClass.toString()
                                            )
                                        }
                                    }
                                })
        )
    }

    private fun addWindScribeToList(checked: Boolean) {
        val pm = appContext.packageManager
        val packageName = appContext.packageName
        try {
            val applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val mData = InstalledAppsData(
                    pm.getApplicationLabel(applicationInfo).toString(),
                    applicationInfo.packageName, pm.getApplicationIcon(applicationInfo), false
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
            PROTO_IKev2 -> interactor.getIKev2Port()
            PROTO_UDP -> interactor.getSavedUDPPort()
            PROTO_TCP -> interactor.getSavedTCPPort()
            PROTO_STEALTH -> interactor.getSavedSTEALTHPort()
            PROTO_WS_TUNNEL -> interactor.getSavedWSTunnelPort()
            PROTO_WIRE_GUARD -> interactor.getWireGuardPort()
            else -> "443"
        }
    }

    private fun modifyList(savedApps: List<String>) {
        val pm = appContext.packageManager
        interactor.getCompositeDisposable().add(
                Single.fromCallable {
                    pm.getInstalledApplications(
                            PackageManager.GET_META_DATA
                    )
                }.flatMap { packages: List<ApplicationInfo> ->
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
                    Single.fromCallable { installedAppList }
                }.observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(object : DisposableSingleObserver<List<InstalledAppsData?>?>() {
                            override fun onError(e: Throwable) {
                                settingView.hideProgress()
                            }

                            override fun onSuccess(packages: List<InstalledAppsData?>) {
                                settingView.hideProgress()
                                installedAppsAdapter = InstalledAppsAdapter(
                                        installedAppList, this@SettingsPresenterImp
                                )
                                installedAppsAdapter?.let {
                                    settingView.setupAppsAdapter(it)
                                }
                            }
                        })
        )
    }

    private fun saveApps(
            savedList: List<String>,
            updatedApp: InstalledAppsData,
            reloadAdapter: Boolean
    ) {
        val list = savedList.toMutableList()
        if (updatedApp.isChecked) {
            list.add(updatedApp.packageName)
        } else {
            list.remove(updatedApp.packageName)
        }
        getExecutorService()
                .submit {
                    interactor.getAppPreferenceInterface().saveInstalledApps(list)
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
                                interactor.getResourceString(R.string.reset_date),
                                formatter.format(nextResetDate)
                        )
                    } else {
                        val nextResetDate = c.time
                        settingView.setResetDate(
                                interactor.getResourceString(R.string.expiry_date),
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
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                portMapResponse.let {
                    val protocol = getProtocolFromHeading(portMapResponse, heading)
                    val savedPort = getSavedPort(protocol)
                    for (portMap in portMapResponse.portmap) {
                        if (portMap.protocol == protocol) {
                            settingView.setupPortMapAdapter(savedPort, portMap.ports)
                        }
                    }
                }
            }
        })
    }

    private fun setProtocolAdapter(savedProtocol: String) {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                portMapResponse.let {
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
        })
    }

    private fun setupAppListAdapter() {
        settingView.showProgress(interactor.getResourceString(R.string.loading))
        interactor.getCompositeDisposable()
                .add(
                        interactor.getAppPreferenceInterface().installedApps
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeOn(Schedulers.io())
                                .subscribeWith(object : DisposableSingleObserver<List<String>>() {
                                    override fun onError(e: Throwable) {
                                        settingView.hideProgress()
                                        modifyList(emptyList())
                                    }

                                    override fun onSuccess(installedAppsData: List<String>) {
                                        modifyList(installedAppsData)
                                    }
                                })
                )
    }
}
