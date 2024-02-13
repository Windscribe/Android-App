/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.tv.windscribe

import android.net.ConnectivityManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import com.windscribe.tv.R.color
import com.windscribe.tv.R.string
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.sort.ByLatency
import com.windscribe.tv.sort.ByRegionName
import com.windscribe.tv.windscribe.WindscribeView.ConnectionStateAnimationListener
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.Util.getModifiedIpAddress
import com.windscribe.vpn.backend.Util.getSavedLocation
import com.windscribe.vpn.backend.Util.validIpAddress
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.model.User
import com.windscribe.vpn.serverlist.entity.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class WindscribePresenterImpl @Inject constructor(
    var windscribeView: WindscribeView,
    var interactor: ActivityInteractor
) : WindscribePresenter, ConnectionStateAnimationListener {

    private val logger = LoggerFactory.getLogger("windscribe_p")
    private var canQuit = false
    private val serverListUpdate = AtomicBoolean()
    private var selectedLocation: LastSelectedLocation? = null
    private var lastVPNState = VPNState.Status.Disconnected

    override suspend fun observeVPNState() {
        interactor.getVpnConnectionStateManager().state.collect { vpnState: VPNState ->
            if (vpnState.status === lastVPNState) return@collect
            logger.info("Setting Connection UI: ${lastVPNState.name}")
            lastVPNState = vpnState.status
            when (vpnState.status) {
                VPNState.Status.Connected -> {
                    vpnState.ip?.let {
                        vpnConnected(it)
                    }
                }
                VPNState.Status.Connecting -> vpnConnecting()
                VPNState.Status.Disconnected -> vpnDisconnected()
                VPNState.Status.Disconnecting -> vpnDisconnecting()
                VPNState.Status.ProtocolSwitch -> {}
                VPNState.Status.InvalidSession -> windscribeView.gotoLoginRegistrationActivity()
                else -> {}
            }
        }
    }

    override fun observeUserState(windscribeActivity: WindscribeActivity) {
        interactor.getUserRepository().user.observe(windscribeActivity) {
            setAccountStatus(it)
            setUserStatus(it)
        }
    }

    override suspend fun observeDisconnectedProtocol() {
        interactor.getAutoConnectionManager().nextInLineProtocol.collectLatest { protocol ->
            if (interactor.getVpnConnectionStateManager().isVPNActive().not()) {
                protocol?.let {
                    windscribeView.setProtocolAndPortInfo(Util.getProtocolLabel(it.protocol), it.port, true)
                }
            }
        }
    }

    override suspend fun observeConnectedProtocol() {
        interactor.getAutoConnectionManager().connectedProtocol.collectLatest { protocol ->
            if (interactor.getVpnConnectionStateManager().isVPNActive()) {
                protocol?.let {
                    windscribeView.setProtocolAndPortInfo(Util.getProtocolLabel(it.protocol), it.port, false)
                }
            }
        }
    }

    override fun onDestroy() {
        logger.info("Disposing observer...")
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun connectWithSelectedStaticIp(
        regionID: Int,
        serverCredentialsResponse: ServerCredentialsResponse
    ) {
        interactor.getAppPreferenceInterface().globalUserConnectionPreference = true
        interactor.getCompositeDisposable().add(
            interactor.getStaticRegionByID(regionID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<StaticRegion?>() {
                    override fun onError(e: Throwable) {
                        logger.info("Static ip not found...")
                        windscribeView.showToast("Error finding static ip.")
                    }

                    override fun onSuccess(region: StaticRegion) {
                        attemptConnectionUsingIp(region, serverCredentialsResponse)
                    }
                })
        )
    }

    override fun connectWithSelectedLocation(cityID: Int) {
        logger.debug("Getting city data.")
        interactor.getAppPreferenceInterface().globalUserConnectionPreference = true
        interactor.getCompositeDisposable().add(
            interactor.getCityAndRegionByID(cityID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<CityAndRegion>() {
                    override fun onError(e: Throwable) {
                        logger.debug("Could not find selected location in database.")
                        windscribeView.showToast("Error")
                    }

                    override fun onSuccess(cityAndRegion: CityAndRegion) {
                        attemptConnection(cityAndRegion)
                    }
                })
        )
    }

    private fun handleRateDialog() {
        if (elapsedOneDayAfterLogin()) {
            interactor.getCompositeDisposable().add(
                interactor.getUserSessionData()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (!interactor.isUserEligibleForRatingApp(it)) {
                            return@subscribe
                        }
                        when (interactor.getRateAppPreference()) {
                            RateDialogConstants.STATUS_DEFAULT -> {
                                interactor.setRateDialogUpdateTime()
                                windscribeView.handleRateView()
                                logger.debug("Rate dialog is being shown for first time.")
                            }
                            RateDialogConstants.STATUS_ASK_LATER -> {
                                val time = interactor.getLastTimeUpdated()
                                val difference = Date().time - time.toLong()
                                val days = TimeUnit.DAYS.convert(difference, MILLISECONDS)
                                if (days >= RateDialogConstants.MINIMUM_DAYS_TO_SHOW_AGAIN) {
                                    interactor.saveRateAppPreference(
                                        RateDialogConstants.STATUS_ALREADY_ASKED
                                    )
                                    windscribeView.handleRateView()
                                    logger
                                        .debug("Rate dialog is being shown and user's last choice was ask me later 90+ days ago.")
                                }
                            }
                            else -> {}
                        }
                    }, {})
            )
        }
    }

    override fun init() {
        val ipAddress = interactor.getAppPreferenceInterface()
            .getResponseString(PreferencesKeyConstants.USER_IP)
        if (ipAddress != null && interactor.getVpnConnectionStateManager().isVPNActive()) {
            logger.info("Setting up user ip address...")
            windscribeView.setIpAddress(ipAddress)
        }
        if (!interactor.getVpnConnectionStateManager().isVPNActive()) {
            setIPAddress()
        }
        windscribeView.startSessionServiceScheduler()
        serverListUpdate.set(false)
        setSelectedLocation()
        addNotificationChangeListener()
        if (problematicBrand) {
            logger.info(Build.MANUFACTURER)
            logger.info("Trying to connect using ethernet on android 9. Showing Error dialog.")
            windscribeView.showErrorDialog(
                "Android 9 has a known issue of ethernet connection failing for VPN apps. Please use Wi-Fi for now. If connection on ethernet works Do let us know."
            )
        }
    }

    override fun logout() {
        interactor.getMainScope().launch {
            interactor.getUserRepository().logout()
        }
    }

    override fun onBackPressed() {
        if (canQuit) {
            windscribeView.quitApplication()
            return
        } else {
            windscribeView.showToast("Click back again to exit")
            canQuit = true
        }
        interactor.getMainScope().launch {
            delay(1000)
            canQuit = false
        }
    }

    override fun onConnectClicked() {
        val vpnStatus = interactor.getVpnConnectionStateManager().state.value.status
        if (vpnStatus == VPNState.Status.Connecting || vpnStatus == VPNState.Status.Connected) {
            logger.debug("connection state was connected or connecting user initiated disconnect")
            disconnectFromVPN()
            return
        }
        if (isAccountStatusOkay) {
            if (!interactor.getVpnConnectionStateManager().isVPNActive()) {
                if (WindUtilities.isOnline()) {
                    logger.debug("connection state was disconnected user initiated connect")
                    startVpnConnectionProcess()
                } else {
                    logger.debug("connection state was disconnected user initiated connect but no network available..")
                    windscribeView.showToast("Please connect to a network first and retry!")
                }
            }
        }
    }

    override fun onConnectedAnimationCompleted() {
        interactor.getAppPreferenceInterface()
        logger.info("Vpn connected animation completed. Setting IP Address")
        windscribeView.showSplitViewIcon(interactor.getAppPreferenceInterface().lastConnectedUsingSplit)
    }

    override fun onConnectingAnimationCompleted() {
        windscribeView.setupLayoutConnecting()
    }

    override fun onDisconnectIntentReceived() {
        logger.info("Setting global connection intent to [FALSE] - Disconnect intent")
        interactor.getAppPreferenceInterface().isReconnecting = false
        interactor.getAppPreferenceInterface().globalUserConnectionPreference = false
        logger.info("Stopping established vpn connection for disconnect intent...")
        interactor.getMainScope().launch { interactor.getVPNController().disconnectAsync() }
    }

    override fun onHotStart() {
        handleRateDialog()
    }

    // UI Items onClick Methods
    override fun onMenuButtonClicked() {
        logger.info("Opening main menu activity...")
        windscribeView.openMenuActivity()
    }

    override fun onNetworkStateChanged() {
        if (!interactor.getVpnConnectionStateManager().isVPNActive()) {
            logger.debug("Network state changed & vpn is not active, getting ip address...")
            setIPAddress()
            if (WindUtilities.isOnline() && !interactor.getVpnConnectionStateManager().isVPNActive() && interactor.getAppPreferenceInterface().pingTestRequired) {
                interactor.getWorkManager().updateNodeLatencies()
            }
        }
    }

    private fun vpnConnecting() {
        windscribeView.setState(1)
        windscribeView.setGlowVisibility(View.GONE)
        windscribeView.setVpnButtonState()
        windscribeView.setProtocolAndPortInfo(Util.getProtocolLabel(interactor.getAppPreferenceInterface().selectedProtocol), interactor.getAppPreferenceInterface().selectedPort,false)
        selectedLocation?.let {
            windscribeView.startVpnConnectingAnimation(
                interactor.getResourceString(string.ON),
                FlagIconResource.getFlag(it.countryCode),
                interactor.getColorResource(color.colorDeepBlue),
                interactor.getColorResource(color.colorNavyBlue),
                interactor.getColorResource(color.colorWhite50),
                interactor.getColorResource(color.sea_green),
                this@WindscribePresenterImpl
            )
        } ?: kotlin.run {
            interactor.getCompositeDisposable().add(
                getSavedLocation()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<LastSelectedLocation?>() {
                        override fun onError(e: Throwable) {
                            logger.debug(e.message)
                        }

                        override fun onSuccess(location: LastSelectedLocation) {
                            selectedLocation = location
                            windscribeView.startVpnConnectingAnimation(
                                interactor.getResourceString(string.on),
                                FlagIconResource.getFlag(location.countryCode),
                                interactor.getColorResource(color.colorDeepBlue),
                                interactor.getColorResource(color.colorNavyBlue),
                                interactor.getColorResource(color.colorWhite50),
                                interactor.getColorResource(color.sea_green),
                                this@WindscribePresenterImpl
                            )
                        }
                    })
            )
        }
    }

    private fun vpnDisconnected() {
        if (interactor.getAppPreferenceInterface().isReconnecting) {
            return
        }
        windscribeView.setupLayoutDisconnected()
        windscribeView.networkInfo?.let {
            if (it.isConnected) {
                setIPAddress()
            }
        }
    }

    private fun vpnDisconnecting() {
        if (interactor.getAppPreferenceInterface().isReconnecting) {
            return
        }
        windscribeView.setState(0)
        windscribeView.setGlowVisibility(View.GONE)
        windscribeView.setVpnButtonState()
        windscribeView.setupLayoutDisconnecting()
    }

    private fun vpnConnected(ip: String) {
        windscribeView.setIpAddress(ip.trim())
        logger.info("Connection with the server is established.")
        windscribeView.startVpnConnectedAnimation(
            interactor.getResourceString(string.ON),
            interactor.getColorResource(color.colorNavyBlue),
            interactor.getColorResource(color.colorPrimary),
            interactor.getColorResource(color.colorLightBlue),
            interactor.getColorResource(color.sea_green),
            this@WindscribePresenterImpl
        )
        updateLocationData(selectedLocation,true)
    }

    override suspend fun observeServerList() {
        interactor.getServerListUpdater().regions.collectLatest {
            setPartialOverlayView()
        }
    }

    private fun setPartialOverlayView() {
        logger.debug("Loading server list.")
        windscribeView.showPartialViewProgress(true)
        val regions: MutableList<RegionAndCities> = ArrayList()
        val dataDetails = ServerListData()
        val oneTimeCompositeDisposable = CompositeDisposable()
        oneTimeCompositeDisposable.add(
            interactor.getAllRegion()
                .flatMap {
                    logger.info("Loaded server regions.")
                    regions.clear()
                    regions.addAll(it)
                    interactor.getAllPings()
                }.onErrorReturnItem(ArrayList())
                .flatMap {
                    logger.info("Loaded ping times.")
                    dataDetails.pingTimes = it
                    interactor.getFavourites()
                }.onErrorReturnItem(ArrayList())
                .flatMap {
                    logger.info("Loaded favourites items.")
                    dataDetails.favourites = it
                    interactor.getLocationProvider().bestLocation
                }.flatMap {
                    dataDetails.setShowLatencyInMs(interactor.getAppPreferenceInterface().showLatencyInMS)
                    dataDetails.bestLocation = it
                    dataDetails.isProUser = interactor.getAppPreferenceInterface().userStatus == 1
                    if (selectedLocation == null) {
                        selectedLocation = LastSelectedLocation(
                            it.city.getId(),
                            it.city.nodeName, it.city.nickName,
                            it.region.countryCode, "", ""
                        )
                        selectedLocation?.let {
                            location -> Util.saveSelectedLocation(location)
                            interactor.getLocationProvider().setSelectedCity(locationId = location.cityId)
                        }
                    }
                    for (regionAndCity in regions) {
                        val total = getTotalPingTime(regionAndCity.cities, dataDetails)
                        regionAndCity.latencyTotal = total
                    }
                    if (interactor.getAppPreferenceInterface().selection == PreferencesKeyConstants.LATENCY_LIST_SELECTION_MODE) {
                        Collections.sort(regions, ByLatency())
                    } else if (interactor.getAppPreferenceInterface().selection == PreferencesKeyConstants.AZ_LIST_SELECTION_MODE) {
                        Collections.sort(regions, ByRegionName())
                    }
                    Single.fromCallable { regions.subList(0, 5.coerceAtMost(regions.size)) }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<RegionAndCities>>() {
                    override fun onError(e: Throwable) {
                        windscribeView.showPartialViewProgress(false)
                        logger.debug("Failed to get server list from database.")
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }

                    override fun onSuccess(regionsList: List<RegionAndCities>) {
                        logger.debug("Successfully loaded server list.")
                        windscribeView.showPartialViewProgress(false)
                        updateLocationData(selectedLocation, true)
                        if (regions.isNotEmpty()) {
                            regions.add(0, RegionAndCities())
                            val serverAdapter = ServerAdapter(regions, dataDetails, null, false)
                            windscribeView.setPartialAdapter(serverAdapter)
                            logger.debug("Partial Server view loaded successfully. ")
                        }
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }
                })
        )
    }

    override suspend fun observeSelectedLocation() {
        interactor.getLocationProvider().selectedCity.collectLatest {
            setSelectedLocation()
        }
    }

    private fun setSelectedLocation() {
        interactor.getCompositeDisposable().add(
            getSavedLocation()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<LastSelectedLocation?>() {
                    override fun onError(e: Throwable) {
                        logger.debug("No last connected location found.")
                        updateLocationData(null, false)
                    }

                    override fun onSuccess(location: LastSelectedLocation) {
                        interactor.getLocationProvider().setSelectedCity(location.cityId)
                        selectedLocation = location
                        updateLocationData(selectedLocation, true)
                    }
                })
        )
    }

    private fun addNotificationChangeListener() {
        logger.debug("Registering notification listener.")
        interactor.getCompositeDisposable().add(
            interactor.getNotifications(interactor.getAppPreferenceInterface().userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSubscriber<List<PopupNotificationTable>>() {
                    override fun onComplete() {
                        logger.debug("Registering notification listener finishing.")
                    }

                    override fun onError(t: Throwable) {
                        logger.debug(
                            "Error reading popup notification table. StackTrace: " +
                                    WindError.instance.convertThrowableToString(t)
                        )
                    }

                    override fun onNext(popupNotificationTables: List<PopupNotificationTable>) {
                        logger.debug("Notification data changed.")
                        checkForPopNotification(popupNotificationTables)
                    }
                })
        )
    }

    private fun attemptConnection(cityAndRegion: CityAndRegion) {
        if (isLocationNotAvailableToUser(cityAndRegion.city.pro == 1)) {
            logger.info("Location selected is a pro node location, opening upgrade dialog...")
            windscribeView.openUpgradeActivity()
            return
        }
        if (WindUtilities.isOnline()) {
            interactor.getAppPreferenceInterface().setConnectingToStaticIP(false)
            selectedLocation = LastSelectedLocation(
                cityAndRegion.city.getId(),
                cityAndRegion.city.nodeName,
                cityAndRegion.city.nickName, cityAndRegion.region.countryCode
            )
            selectedLocation?.let {
                Util.saveSelectedLocation(it)
                interactor.getLocationProvider().setSelectedCity(it.cityId)
            }
            interactor.getVPNController().connectAsync()
        } else {
            logger.info("User not connected to any network.")
            windscribeView.showToast("Please connect to a network first and retry!")
        }
    }

    private fun attemptConnectionUsingIp(
        staticRegion: StaticRegion,
        serverCredentialsResponse: ServerCredentialsResponse
    ) {
        if (WindUtilities.isOnline()) {
            logger.info("User clicked on static ip: " + staticRegion.cityName)
            interactor.getAppPreferenceInterface().setConnectingToStaticIP(true)
            // Saving static IP credentials
            interactor.getAppPreferenceInterface().saveCredentials(
                PreferencesKeyConstants.STATIC_IP_CREDENTIAL,
                serverCredentialsResponse
            )
            selectedLocation = LastSelectedLocation(
                staticRegion.id, staticRegion.cityName,
                staticRegion.staticIp, staticRegion.countryCode
            )
            selectedLocation?.let {
                Util.saveSelectedLocation(it)
                interactor.getLocationProvider().setSelectedCity(it.cityId)
            }
            interactor.getMainScope().launch {
                interactor.getAutoConnectionManager().connectInForeground()
            }
        } else {
            logger.info("User not connected to any network.")
            windscribeView.showToast("Please connect to a network first and retry!")
        }
    }

    private fun checkForPopNotification(popupNotificationTables: List<PopupNotificationTable>) {
        for (popupNotification in popupNotificationTables) {
            val alreadySeen = interactor.getAppPreferenceInterface()
                .isNotificationAlreadyShown(popupNotification.notificationId.toString())
            if (!alreadySeen && popupNotification.popUpStatus == 1) {
                logger.info("New popup notification received, showing notification...")
                interactor.getAppPreferenceInterface()
                    .saveNotificationId(popupNotification.notificationId.toString())
                windscribeView.openNewsFeedActivity(true, popupNotification.notificationId)
                break
            }
        }
    }

    private fun elapsedOneDayAfterLogin(): Boolean {
        val milliSeconds1 = interactor.getAppPreferenceInterface().loginTime.time
        val milliSeconds2 = Date().time
        val periodSeconds = (milliSeconds2 - milliSeconds1) / 1000
        val elapsedDays = periodSeconds / 60 / 60 / 24
        return elapsedDays > 0
    }

    private fun setIPAddress() {
        if (WindUtilities.isOnline()) {
            logger.info("Getting ip address from Api.")
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager()
                    .checkConnectivityAndIpAddress()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        it.dataClass?.let { ip ->
                            if (validIpAddress(ip.trim())) {
                                windscribeView.setIpAddress(getModifiedIpAddress(ip.trim { it <= ' ' }))
                            } else {
                                logger.info("Server returned error response when getting user ip")
                                windscribeView.setIpAddress("---.---.---.---")
                            }
                        } ?: kotlin.run {
                            logger.info("Setting up user ip address...")
                            windscribeView.setIpAddress("---.---.---.---")
                        }
                    }, {
                        logger.debug("Network call to get ip failed.")
                        windscribeView.setIpAddress("---.---.---.---")
                    })
            )
        } else {
            logger.debug("Network is not available. Ip update failed...")
            windscribeView.setIpAddress("---.---.---.---")
        }
    }

    private fun getTotalPingTime(cities: List<City>, dataDetails: ServerListData): Int {
        var total = 0
        var index = 0
        for (city in cities) {
            for (pingTime in dataDetails.pingTimes) {
                if (pingTime.id == city.getId()) {
                    total += pingTime.getPingTime()
                    index++
                }
            }
        }
        if (index == 0) {
            return 2000
        }
        val average = total / index
        return if (average == -1) {
            2000
        } else average
    }

    private val isAccountStatusOkay: Boolean
        get() {
            when (interactor.getUserRepository().user.value?.accountStatus) {
                User.AccountStatus.Okay -> {
                    logger.debug("Account status was okay")
                    return true
                }
                User.AccountStatus.Expired -> {
                    logger.debug("Account status was expired")
                    windscribeView.openUpgradeActivity()
                }
                User.AccountStatus.Banned -> {
                    logger.debug("Account status was banned")
                    windscribeView.setupAccountStatusBanned()
                }
                else -> {}
            }
            return false
        }

    private val problematicBrand: Boolean
        get() {
            val problematicBrands =
                Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) or Build.MANUFACTURER
                    .equals("Philips", ignoreCase = true)
            return problematicBrands && VERSION.SDK_INT == VERSION_CODES.P && windscribeView.networkInfo != null && windscribeView.networkInfo?.type == ConnectivityManager.TYPE_ETHERNET
        }

    private fun isLocationNotAvailableToUser(pro: Boolean): Boolean {
        return pro && interactor.getAppPreferenceInterface().userStatus != UserStatusConstants.USER_STATUS_PREMIUM
    }

    private fun disconnectFromVPN() {
        interactor.getMainScope().launch {
            interactor.getAppPreferenceInterface().setUserIntendedDisconnect(true)
            interactor.getAppPreferenceInterface().globalUserConnectionPreference = false
            interactor.getAppPreferenceInterface().isReconnecting = false
            interactor.getVPNController().disconnectAsync()
        }
    }

    private fun startVpnConnectionProcess() {
        if (selectedLocation != null) {
            isLocationNotAvailableToUser(false)
            interactor.getAppPreferenceInterface().globalUserConnectionPreference = true
            logger.info("VPN connection attempt started...")
            interactor.getMainScope().launch {
                interactor.getAutoConnectionManager().connectInForeground()
            }
        } else {
            logger.info("Selected location is null!")
            windscribeView.showToast(interactor.getResourceString(string.select_location))
        }
    }

    private fun updateLocationData(
        lastSelectedLocation: LastSelectedLocation?,
        updateFlag: Boolean
    ) {
        if (lastSelectedLocation != null) {
            val lowestPingId = interactor.getAppPreferenceInterface().lowestPingId
            if (lowestPingId == lastSelectedLocation.cityId) {
                lastSelectedLocation.nodeName = interactor.getResourceString(string.best_location)
            }
            windscribeView.updateLocationName(
                lastSelectedLocation.nodeName,
                lastSelectedLocation.nickName
            )
            if (updateFlag) {
                windscribeView.setCountryFlag(FlagIconResource.getFlag(lastSelectedLocation.countryCode))
            }
        }
    }

    private fun setAccountStatus(user: User) {
        when (user.accountStatus) {
            User.AccountStatus.Okay -> {}
            User.AccountStatus.Banned -> {
                if (interactor.getVpnConnectionStateManager().isVPNActive()) {
                    interactor.getMainScope()
                        .launch { interactor.getVPNController().disconnectAsync() }
                }
                windscribeView.setupAccountStatusBanned()
            }
            else -> {
                val previousAccountStatus =
                    interactor.getAppPreferenceInterface().getPreviousAccountStatus(user.userName)
                if (user.accountStatusToInt != previousAccountStatus) {
                    interactor.getAppPreferenceInterface()
                        .setPreviousAccountStatus(user.userName, user.accountStatusToInt)
                    if (user.accountStatus == User.AccountStatus.Expired) {
                        setUserStatus(user)
                        if (interactor.getVpnConnectionStateManager().isVPNActive()) {
                            interactor.getMainScope()
                                .launch { interactor.getVPNController().disconnectAsync() }
                        }
                        windscribeView.setupAccountStatusExpired()
                    }
                }
            }
        }
    }

    private fun setUserStatus(user: User) {
        if (user.maxData != -1L) {
            user.dataLeft?.let {
                val dataRemaining = interactor.getDataLeftString(string.data_left, it)
                windscribeView.setupLayoutForFreeUser(
                    dataRemaining,
                    getDataRemainingColor(it, user.maxData)
                )
            }
        } else {
            windscribeView.setupLayoutForProUser()
        }
    }

    private fun getDataRemainingColor(dataRemaining: Float, maxData: Long): Int {
        return if (maxData != -1L) when {
            dataRemaining < BillingConstants.DATA_LOW_PERCENTAGE * (
                    maxData /
                            UserStatusConstants.GB_DATA.toFloat()
                    ) -> interactor.getColorResource(color.colorRed)
            dataRemaining
                    < BillingConstants.DATA_WARNING_PERCENTAGE * (maxData / UserStatusConstants.GB_DATA.toFloat()) ->
                interactor
                    .getColorResource(color.colorYellow)
            else ->
                interactor
                    .getColorResource(color.colorWhite)
        } else interactor.getColorResource(color.colorWhite)
    }
}
