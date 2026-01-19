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
import com.windscribe.tv.di.PerActivity
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.sort.ByLatency
import com.windscribe.tv.sort.ByRegionName
import com.windscribe.tv.windscribe.WindscribeView.ConnectionStateAnimationListener
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.Util.getModifiedIpAddress
import com.windscribe.vpn.backend.Util.validIpAddress
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.serverlist.entity.*
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@PerActivity
class WindscribePresenterImpl @Inject constructor(
    var windscribeView: WindscribeView,
    private val activityScope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface,
    private val userRepository: UserRepository,
    private val serverListRepository: ServerListRepository,
    private val locationRepository: LocationRepository,
    private val autoConnectionManager: AutoConnectionManager,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val vpnController: WindVpnController,
    private val workManager: WindScribeWorkManager,
    private val deviceStateManager: DeviceStateManager,
    private val resourceHelper: ResourceHelper
) : WindscribePresenter, ConnectionStateAnimationListener {

    private val logger = LoggerFactory.getLogger("basic")
    private var canQuit = false
    private val serverListUpdate = AtomicBoolean()
    private var selectedLocation: LastSelectedLocation? = null
    private var lastVPNState = VPNState.Status.Disconnected

    override suspend fun observeVPNState() {
        vpnConnectionStateManager.state.collect { vpnState: VPNState ->
            if (vpnState.status === lastVPNState) return@collect
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
        userRepository.user.observe(windscribeActivity) {
            setAccountStatus(it)
            setUserStatus(it)
        }
    }

    override suspend fun observeDisconnectedProtocol() {
        autoConnectionManager.nextInLineProtocol.collectLatest { protocol ->
            if (vpnConnectionStateManager.isVPNActive().not()) {
                protocol?.let {
                    windscribeView.setProtocolAndPortInfo(Util.getProtocolLabel(it.protocol), it.port, true)
                }
            }
        }
    }

    override suspend fun observeConnectedProtocol() {
        autoConnectionManager.connectedProtocol.collectLatest { protocol ->
            if (vpnConnectionStateManager.isVPNActive()) {
                protocol?.let {
                    windscribeView.setProtocolAndPortInfo(Util.getProtocolLabel(it.protocol), it.port, false)
                }
            }
        }
    }

    override fun onDestroy() {
        logger.info("Cleaning up...")
        // Coroutines launched with activityScope are automatically cancelled when the scope is cancelled
    }

    override fun connectWithSelectedStaticIp(
        regionID: Int,
        serverCredentialsResponse: ServerCredentialsResponse
    ) {
        preferencesHelper.globalUserConnectionPreference = true
        activityScope.launch {
            try {
                val region = localDbInterface.getStaticRegionByIDAsync(regionID)
                if (region != null) {
                    attemptConnectionUsingIp(region, serverCredentialsResponse)
                } else {
                    logger.info("Static ip not found...")
                    windscribeView.showToast("Error finding static ip.")
                }
            } catch (_: Exception) {
                logger.info("Static ip not found...")
                windscribeView.showToast("Error finding static ip.")
            }
        }
    }

    override fun connectWithSelectedLocation(cityID: Int) {
        preferencesHelper.globalUserConnectionPreference = true
        activityScope.launch {
            try {
                val cityAndRegion = withContext(Dispatchers.IO) {
                    localDbInterface.getCityAndRegion(cityID)
                }
                if (cityAndRegion != null) {
                    attemptConnection(cityAndRegion)
                } else {
                    logger.debug("Could not find selected location in database.")
                    windscribeView.showToast("Could not find selected location in database.")
                }
            } catch (_: Exception) {
                logger.debug("Could not find selected location in database.")
                windscribeView.showToast("Error")
            }
        }
    }

    private fun handleRateDialog() {
        activityScope.launch {
            try {
                val userSessionResponse = withContext(Dispatchers.IO) {
                    val userSessionString = preferencesHelper.getResponseString(PreferencesKeyConstants.GET_SESSION)
                    com.google.gson.Gson().fromJson(userSessionString, com.windscribe.vpn.api.response.UserSessionResponse::class.java)
                }

                if (!isUserEligibleForRatingApp(userSessionResponse)) {
                    return@launch
                }

                when (getRateAppPreference()) {
                    RateDialogConstants.STATUS_DEFAULT -> {
                        setRateDialogUpdateTime()
                        windscribeView.handleRateView()
                        logger.debug("Rate dialog is being shown for first time.")
                    }
                    RateDialogConstants.STATUS_ASK_LATER -> {
                        val time = getLastTimeUpdated()
                        val difference = Date().time - time.toLong()
                        val days = TimeUnit.DAYS.convert(difference, MILLISECONDS)
                        if (days >= RateDialogConstants.MINIMUM_DAYS_TO_SHOW_AGAIN) {
                            saveRateAppPreference(RateDialogConstants.STATUS_ALREADY_ASKED)
                            windscribeView.handleRateView()
                            logger.debug("Rate dialog is being shown and user's last choice was ask me later 90+ days ago.")
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                // Silently fail for rate dialog
                logger.debug("Failed to check rate dialog eligibility: ${e.message}")
            }
        }
    }

    private fun isUserEligibleForRatingApp(userSessionResponse: com.windscribe.vpn.api.response.UserSessionResponse): Boolean {
        val user = User(userSessionResponse)
        val dataUsed = user.dataUsed.toDouble() / (1024 * 1024 * 1024)
        return dataUsed >= 2.0 && elapsedTwoDayAfterLogin() && lastShownDays()
    }

    private fun elapsedTwoDayAfterLogin(): Boolean {
        val milliSeconds1 = preferencesHelper.loginTime?.time ?: Date().time
        val milliSeconds2 = Date().time
        val periodSeconds = (milliSeconds2 - milliSeconds1) / 1000
        val elapsedDays = periodSeconds / 60 / 60 / 24
        return elapsedDays.toInt() >= 2
    }

    private fun lastShownDays(): Boolean {
        val time = preferencesHelper.getResponseString(RateDialogConstants.LAST_UPDATE_TIME) ?: return true
        return try {
            val difference = Date().time - time.toLong()
            val days = TimeUnit.DAYS.convert(difference, MILLISECONDS)
            days > RateDialogConstants.MINIMUM_DAYS_TO_START
        } catch (_: NumberFormatException) {
            true
        }
    }

    private fun getRateAppPreference(): Int {
        return preferencesHelper.getResponseInt(
            RateDialogConstants.CURRENT_STATUS_KEY,
            RateDialogConstants.STATUS_DEFAULT
        )
    }

    private fun getLastTimeUpdated(): String {
        return preferencesHelper.getResponseString(RateDialogConstants.LAST_UPDATE_TIME)
            ?: Date().time.toString()
    }

    private fun saveRateAppPreference(type: Int) {
        preferencesHelper.saveResponseIntegerData(RateDialogConstants.CURRENT_STATUS_KEY, type)
    }

    private fun setRateDialogUpdateTime() {
        preferencesHelper.saveResponseStringData(
            RateDialogConstants.LAST_UPDATE_TIME,
            Date().time.toString()
        )
    }

    override fun init() {
        val ipAddress = preferencesHelper
            .getResponseString(PreferencesKeyConstants.USER_IP)
        if (ipAddress != null && vpnConnectionStateManager.isVPNActive()) {
            windscribeView.setIpAddress(ipAddress)
        }
        if (!vpnConnectionStateManager.isVPNActive()) {
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
        activityScope.launch {
            userRepository.logout()
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
        activityScope.launch {
            delay(1000)
            canQuit = false
        }
    }

    override fun onConnectClicked() {
        val vpnStatus = vpnConnectionStateManager.state.value.status
        if (vpnStatus == VPNState.Status.Connecting || vpnStatus == VPNState.Status.Connected) {
            logger.debug("connection state was connected or connecting user initiated disconnect")
            disconnectFromVPN()
            return
        }
        if (isAccountStatusOkay) {
            if (!vpnConnectionStateManager.isVPNActive()) {
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
        windscribeView.showSplitViewIcon(preferencesHelper.lastConnectedUsingSplit)
    }

    override fun onConnectingAnimationCompleted() {
        windscribeView.setupLayoutConnecting()
    }

    override fun onDisconnectIntentReceived() {
        logger.info("Setting global connection intent to [FALSE] - Disconnect intent")
        preferencesHelper.isReconnecting = false
        preferencesHelper.globalUserConnectionPreference = false
        logger.info("Stopping established vpn connection for disconnect intent...")
        activityScope.launch { vpnController.disconnectAsync() }
    }

    override fun onHotStart() {
        handleRateDialog()
    }

    // UI Items onClick Methods
    override fun onMenuButtonClicked() {
        logger.info("Opening main menu activity...")
        windscribeView.openMenuActivity()
    }

   override suspend fun observeNetworkEvents() {
       deviceStateManager.isOnline.collect { isOnline ->
           if (!isOnline) {
               logger.debug("Network state changed & vpn is not active, getting ip address...")
               setIPAddress()
               if (WindUtilities.isOnline() && !vpnConnectionStateManager.isVPNActive() && preferencesHelper.pingTestRequired) {
                   workManager.updateNodeLatencies()
               }
           }
       }
    }

    private fun vpnConnecting() {
        windscribeView.setState(1)
        windscribeView.setGlowVisibility(View.GONE)
        windscribeView.setVpnButtonState()
        windscribeView.setProtocolAndPortInfo(Util.getProtocolLabel(preferencesHelper.selectedProtocol), preferencesHelper.selectedPort,false)
        selectedLocation?.let {
            windscribeView.startVpnConnectingAnimation(
                resourceHelper.getString(com.windscribe.vpn.R.string.ON),
                FlagIconResource.getFlag(it.countryCode),
                resourceHelper.getColorResource(color.colorDeepBlue),
                resourceHelper.getColorResource(color.colorNavyBlue),
                resourceHelper.getColorResource(color.colorWhite50),
                resourceHelper.getColorResource(color.sea_green),
                this@WindscribePresenterImpl
            )
        } ?: run {
            activityScope.launch {
                try {
                    val location = withContext(Dispatchers.IO) {
                        Util.getSavedLocationAsync()
                    }
                    selectedLocation = location
                    windscribeView.startVpnConnectingAnimation(
                        resourceHelper.getString(com.windscribe.vpn.R.string.on),
                        FlagIconResource.getFlag(location.countryCode),
                        resourceHelper.getColorResource(color.colorDeepBlue),
                        resourceHelper.getColorResource(color.colorNavyBlue),
                        resourceHelper.getColorResource(color.colorWhite50),
                        resourceHelper.getColorResource(color.sea_green),
                        this@WindscribePresenterImpl
                    )
                } catch (e: Exception) {
                    logger.debug(e.message)
                }
            }
        }
    }

    private fun vpnDisconnected() {
        if (preferencesHelper.isReconnecting) {
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
        if (preferencesHelper.isReconnecting) {
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
            resourceHelper.getString(com.windscribe.vpn.R.string.ON),
            resourceHelper.getColorResource(color.colorNavyBlue),
            resourceHelper.getColorResource(color.colorPrimary),
            resourceHelper.getColorResource(color.colorLightBlue),
            resourceHelper.getColorResource(color.sea_green),
            this@WindscribePresenterImpl
        )
        updateLocationData(selectedLocation,true)
    }

    override suspend fun observeServerList() {
        serverListRepository.regions.collectLatest {
            setPartialOverlayView()
        }
    }

    private fun setPartialOverlayView() {
        activityScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                windscribeView.showPartialViewProgress(true)
            }
            try {
                // Fetch all data on IO dispatcher
                val regionsDeferred = localDbInterface.getAllRegionAsync()
                val pingsDeferred = localDbInterface.getAllPingsAsync()
                val favouritesDeferred = localDbInterface.getFavouritesAsync()
                val bestLocationDeferred = locationRepository.getBestLocationAsync()

                // Process data
                val regions: MutableList<RegionAndCities> = ArrayList(regionsDeferred)
                val dataDetails = ServerListData()
                dataDetails.pingTimes = pingsDeferred
                dataDetails.favourites = favouritesDeferred
                dataDetails.setShowLatencyInMs(preferencesHelper.showLatencyInMS)
                dataDetails.bestLocation = bestLocationDeferred
                dataDetails.isProUser = preferencesHelper.userStatus == 1

                if (selectedLocation == null) {
                    selectedLocation = LastSelectedLocation(
                        bestLocationDeferred.city.getId(),
                        bestLocationDeferred.city.nodeName,
                        bestLocationDeferred.city.nickName,
                        bestLocationDeferred.region.countryCode,
                        "",
                        ""
                    )
                    selectedLocation?.let { location ->
                        Util.saveSelectedLocation(location)
                        locationRepository.setSelectedCity(locationId = location.cityId)
                    }
                }

                for (regionAndCity in regions) {
                    val total = getTotalPingTime(regionAndCity.cities, dataDetails)
                    regionAndCity.latencyTotal = total
                }

                when (preferencesHelper.selection) {
                    PreferencesKeyConstants.LATENCY_LIST_SELECTION_MODE -> {
                        Collections.sort(regions, ByLatency())
                    }
                    PreferencesKeyConstants.AZ_LIST_SELECTION_MODE -> {
                        Collections.sort(regions, ByRegionName())
                    }
                }
                withContext(Dispatchers.Main) {
                    windscribeView.showPartialViewProgress(false)
                    updateLocationData(selectedLocation, true)
                    if (regions.isNotEmpty()) {
                        regions.add(0, RegionAndCities())
                        val serverAdapter = ServerAdapter(regions, dataDetails, null, false)
                        windscribeView.setPartialAdapter(serverAdapter)
                        logger.debug("Partial Server view loaded successfully. ")
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    windscribeView.showPartialViewProgress(false)
                }
                logger.debug("Failed to get server list from database.")
            }
        }
    }

    override suspend fun observeSelectedLocation() {
        locationRepository.selectedCity.collectLatest {
            setSelectedLocation()
        }
    }

    private fun setSelectedLocation() {
        activityScope.launch {
            try {
                val location = withContext(Dispatchers.IO) {
                    Util.getSavedLocationAsync()
                }
                locationRepository.setSelectedCity(location.cityId)
                selectedLocation = location
                updateLocationData(selectedLocation, true)
            } catch (_: Exception) {
                updateLocationData(null, false)
            }
        }
    }

    private fun addNotificationChangeListener() {
        activityScope.launch(Dispatchers.IO) {
            try {
                localDbInterface.getPopupNotificationsAsFlow(preferencesHelper.userName)
                    .collect { popupNotificationTables ->
                        withContext(Dispatchers.Main) {
                            checkForPopNotification(popupNotificationTables)
                        }
                    }
                logger.debug("Registering notification listener finishing.")
            } catch (t: Throwable) {
                logger.debug(
                    "Error reading popup notification table. StackTrace: " +
                            WindError.instance.convertThrowableToString(t)
                )
            }
        }
    }

    private fun attemptConnection(cityAndRegion: CityAndRegion) {
        if (isLocationNotAvailableToUser(cityAndRegion.city.pro == 1)) {
            logger.info("Location selected is a pro node location, opening upgrade dialog...")
            windscribeView.openUpgradeActivity()
            return
        }
        if (WindUtilities.isOnline()) {
            preferencesHelper.setConnectingToStaticIP(false)
            selectedLocation = LastSelectedLocation(
                cityAndRegion.city.getId(),
                cityAndRegion.city.nodeName,
                cityAndRegion.city.nickName, cityAndRegion.region.countryCode
            )
            selectedLocation?.let {
                Util.saveSelectedLocation(it)
                locationRepository.setSelectedCity(it.cityId)
            }
            vpnController.connectAsync()
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
            preferencesHelper.setConnectingToStaticIP(true)
            // Saving static IP credentials
            preferencesHelper.saveCredentials(
                PreferencesKeyConstants.STATIC_IP_CREDENTIAL,
                serverCredentialsResponse
            )
            selectedLocation = LastSelectedLocation(
                staticRegion.id, staticRegion.cityName,
                staticRegion.staticIp, staticRegion.countryCode
            )
            selectedLocation?.let {
                Util.saveSelectedLocation(it)
                locationRepository.setSelectedCity(it.cityId)
            }
            activityScope.launch {
                autoConnectionManager.connectInForeground()
            }
        } else {
            logger.info("User not connected to any network.")
            windscribeView.showToast("Please connect to a network first and retry!")
        }
    }

    private fun checkForPopNotification(popupNotificationTables: List<PopupNotificationTable>) {
        activityScope.launch(Dispatchers.IO) {
            // Find the first notification that should be shown
            val notificationToShow = popupNotificationTables.firstOrNull { popupNotification ->
                val alreadySeen =
                    localDbInterface.isNotificationRead(popupNotification.notificationId)
                !alreadySeen && popupNotification.popUpStatus == 1
            }

            // If we found a notification to show, mark it as shown and open the activity
            notificationToShow?.let { notification ->
                logger.info("New popup notification received, showing notification...")
                // Mark popup as shown (set popUpStatus = 0) so it won't show again
                localDbInterface.markPopupAsShown(notification.notificationId)
                withContext(Dispatchers.Main) {
                    windscribeView.openNewsFeedActivity(true, notification.notificationId)
                }
            }
        }
    }

    private fun setIPAddress() {
        if (WindUtilities.isOnline()) {
            activityScope.launch(Dispatchers.IO) {
                val result = result<String> {
                    apiCallManager.getIp()
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Success -> {
                            if (validIpAddress(result.data)) {
                                windscribeView.setIpAddress(getModifiedIpAddress(result.data.trim { it <= ' ' }))
                            } else {
                                logger.info("Server returned error response when getting user ip")
                                windscribeView.setIpAddress("---.---.---.---")
                            }
                        }
                        is CallResult.Error -> {
                            logger.debug("Network call to get ip failed: ${result.errorMessage}")
                            windscribeView.setIpAddress("---.---.---.---")
                        }
                    }
                }
            }
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
            when (userRepository.user.value?.accountStatus) {
                User.AccountStatus.Okay -> {
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
        return pro && preferencesHelper.userStatus != UserStatusConstants.USER_STATUS_PREMIUM
    }

    private fun disconnectFromVPN() {
        activityScope.launch {
            preferencesHelper.setUserIntendedDisconnect(true)
            preferencesHelper.globalUserConnectionPreference = false
            preferencesHelper.isReconnecting = false
            vpnController.disconnectAsync()
        }
    }

    private fun startVpnConnectionProcess() {
        if (selectedLocation != null) {
            isLocationNotAvailableToUser(false)
            preferencesHelper.globalUserConnectionPreference = true
            activityScope.launch {
                autoConnectionManager.connectInForeground()
            }
        } else {
            logger.info("Selected location is null!")
            windscribeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.select_location))
        }
    }

    private fun updateLocationData(
        lastSelectedLocation: LastSelectedLocation?,
        updateFlag: Boolean
    ) {
        if (lastSelectedLocation != null) {
            val lowestPingId = preferencesHelper.lowestPingId
            if (lowestPingId == lastSelectedLocation.cityId) {
                lastSelectedLocation.nodeName = resourceHelper.getString(com.windscribe.vpn.R.string.best_location)
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
                if (vpnConnectionStateManager.isVPNActive()) {
                    activityScope
                        .launch { vpnController.disconnectAsync() }
                }
                windscribeView.setupAccountStatusBanned()
            }
            else -> {
                val previousAccountStatus =
                    preferencesHelper.getPreviousAccountStatus(user.userName)
                if (user.accountStatusToInt != previousAccountStatus) {
                    preferencesHelper
                        .setPreviousAccountStatus(user.userName, user.accountStatusToInt)
                    if (user.accountStatus == User.AccountStatus.Expired) {
                        setUserStatus(user)
                        if (vpnConnectionStateManager.isVPNActive()) {
                            activityScope
                                .launch { vpnController.disconnectAsync() }
                        }
                        windscribeView.setupAccountStatusExpired()
                    }
                }
            }
        }
    }

    private fun setUserStatus(user: User) {
        if (user.maxData != -1L) {
            user.dataLeft.let {
                val dataRemaining = resourceHelper.getDataLeftString(com.windscribe.vpn.R.string.data_left, it)
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
                    ) -> resourceHelper.getColorResource(color.colorRed)
            dataRemaining
                    < BillingConstants.DATA_WARNING_PERCENTAGE * (maxData / UserStatusConstants.GB_DATA.toFloat()) ->
                resourceHelper.getColorResource(color.colorYellow)
            else ->
                resourceHelper.getColorResource(color.colorWhite)
        } else resourceHelper.getColorResource(color.colorWhite)
    }
}
