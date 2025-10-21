package com.windscribe.mobile.ui.connection

import android.media.MediaPlayer
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper.bundledBackgrounds
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper.getSoundFile
import com.windscribe.mobile.ui.common.isEnabled
import com.windscribe.mobile.ui.home.HomeGoto
import com.windscribe.mobile.ui.serverlist.ServerListItem
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.IpRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.RepositoryState
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.CityAndRegion
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.state.NetworkInfoListener
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject


sealed class LocationInfoState {
    data class Success(val locationInfo: LocationInfo) : LocationInfoState()
    object Unavailable : LocationInfoState()
}

sealed class ToastMessage {
    data class Localized(@StringRes val message: Int) : ToastMessage()
    data class Raw(val message: String) : ToastMessage()
    object None : ToastMessage()
}

sealed class LocationBackground {
    open val resource: Int = R.drawable.dummy_flag

    data class Flag(@DrawableRes override val resource: Int) : LocationBackground()
    data class Wallpaper(@DrawableRes override val resource: Int) : LocationBackground()
    data class Custom(val file: File) : LocationBackground()
}

data class LocationInfo(
    val country: String,
    val nodeName: String,
    val nickName: String,
    val locationBackground: LocationBackground
)

sealed class NetworkInfoState {
    open val name: String? = null

    data class Secured(override val name: String) : NetworkInfoState()
    data class Unsecured(override val name: String) : NetworkInfoState()
    object Unknown : NetworkInfoState()
}

sealed class ConnectionUIState {
    abstract val locationInfo: LocationInfoState?
    abstract val protocolInfo: ProtocolInformation?

    data class Connecting(
        override val protocolInfo: ProtocolInformation,
        override val locationInfo: LocationInfoState
    ) : ConnectionUIState()

    data class Connected(
        override val protocolInfo: ProtocolInformation,
        override val locationInfo: LocationInfoState,
        val connectedUsingSplitRouting: Boolean = false
    ) : ConnectionUIState()

    data class Disconnected(
        override val protocolInfo: ProtocolInformation,
        override val locationInfo: LocationInfoState
    ) : ConnectionUIState()

    object Idle : ConnectionUIState() {
        override val protocolInfo: ProtocolInformation?
            get() = null
        override val locationInfo: LocationInfoState?
            get() = null
    }
}


abstract class ConnectionViewmodel : ViewModel() {
    abstract val connectionUIState: StateFlow<ConnectionUIState>
    abstract val ipState: StateFlow<String>
    abstract val shouldAnimateIp: StateFlow<Boolean>
    abstract val networkInfoState: StateFlow<NetworkInfoState>
    abstract val ipContextMenuState: StateFlow<Pair<Boolean, Offset>>
    abstract val bestLocation: StateFlow<ServerListItem?>
    abstract val isAntiCensorshipEnabled: StateFlow<Boolean>
    abstract val isPreferredProtocolEnabled: StateFlow<Boolean>
    abstract val isDecoyTrafficEnabled: StateFlow<Boolean>
    abstract val aspectRatio: StateFlow<Int>
    abstract val goto: SharedFlow<HomeGoto>
    abstract val newFeedCount: StateFlow<Int>
    abstract val hasPinnedIp: StateFlow<Boolean>
    abstract fun onConnectButtonClick()
    abstract fun onCityClick(city: City)
    abstract fun onStaticIpClick(staticRegion: StaticRegion)
    abstract fun onConfigClick(config: ConfigFile)
    abstract fun onIpContextMenuPosition(position: Offset)
    abstract fun onRotateIpClick()
    abstract fun onPinIPClick()
    abstract fun setContextMenuState(state: Boolean)
    abstract val toastMessage: StateFlow<ToastMessage>
    abstract val isSingleLineLocationName: StateFlow<Boolean>
    abstract val shouldPlayHapticFeedback: StateFlow<Boolean>
    abstract val shouldAnimateFavoriteIcon: StateFlow<Boolean>

    abstract val favouriteIconAnimation: StateFlow<Int>
    abstract fun clearToast()
    abstract fun onProtocolChangeClick()
    abstract fun onGoToHandled()
    abstract fun setIsSingleLineLocationName(singleLine: Boolean)
    abstract fun onHapticFeedbackHandled()
    abstract fun onIpAnimationComplete()
    abstract fun onFavoriteAnimationHandled()
}

class ConnectionViewmodelImpl @Inject constructor(
    private val appScope: CoroutineScope,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val vpnController: WindVpnController,
    private val ipRepository: IpRepository,
    private val networkInfoManager: NetworkInfoManager,
    private val locationRepository: LocationRepository,
    private val localdb: LocalDbInterface,
    private val preferences: PreferencesHelper,
    private val autoConnectionManager: AutoConnectionManager,
    private val userRepository: UserRepository,
    private val serverListRepository: ServerListRepository,
    private val decoyTrafficController: DecoyTrafficController,
    private val api: IApiCallManager
) :
    ConnectionViewmodel() {
    private val _connectionUIState = MutableStateFlow<ConnectionUIState>(ConnectionUIState.Idle)
    override val connectionUIState: StateFlow<ConnectionUIState> = _connectionUIState

    private val _ipState: MutableStateFlow<String> = MutableStateFlow("")
    override val ipState: StateFlow<String> = _ipState
    private val _shouldAnimateIp: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val shouldAnimateIp: StateFlow<Boolean> = _shouldAnimateIp
    private var lastValidIp: String? = null
    private val _networkInfoState: MutableStateFlow<NetworkInfoState> =
        MutableStateFlow(NetworkInfoState.Unknown)
    override val networkInfoState: StateFlow<NetworkInfoState> = _networkInfoState
    private val _ipContextMenuState = MutableStateFlow(Pair(false, Offset.Zero))
    override val ipContextMenuState: StateFlow<Pair<Boolean, Offset>> = _ipContextMenuState
    private val _toastMessage = MutableStateFlow<ToastMessage>(ToastMessage.None)
    override val toastMessage: StateFlow<ToastMessage> = _toastMessage
    private val _bestLocation = MutableStateFlow<ServerListItem?>(null)
    override val bestLocation: StateFlow<ServerListItem?> = _bestLocation
    private val _isAntiCensorshipEnabled = MutableStateFlow(preferences.isAntiCensorshipOn)
    override val isAntiCensorshipEnabled: StateFlow<Boolean> = _isAntiCensorshipEnabled
    private val _isPreferredProtocolEnabled = MutableStateFlow(false)
    override val isPreferredProtocolEnabled: StateFlow<Boolean> = _isPreferredProtocolEnabled
    private val _isDecoyTrafficEnabled = MutableStateFlow(false)
    override val isDecoyTrafficEnabled: StateFlow<Boolean> = _isDecoyTrafficEnabled
    private val _goto = MutableSharedFlow<HomeGoto>(replay = 0)
    override val goto: SharedFlow<HomeGoto> = _goto
    private val _newFeedCount = MutableStateFlow(0)
    override val newFeedCount: StateFlow<Int> = _newFeedCount
    private var preferenceChangeListener: OnTrayPreferenceChangeListener? = null

    private val lastLocationState: MutableStateFlow<LastSelectedLocation?> = MutableStateFlow(null)
    private var networkListener: NetworkInfoListener? = null
    private val _aspectRatio = MutableStateFlow(1)
    override val aspectRatio: StateFlow<Int> = _aspectRatio
    private val _isSingleLineLocationName = MutableStateFlow(true)
    override val isSingleLineLocationName: StateFlow<Boolean> = _isSingleLineLocationName
    private val _shouldPlayHapticFeedback = MutableStateFlow(false)
    override val shouldPlayHapticFeedback: StateFlow<Boolean> = _shouldPlayHapticFeedback
    private val _shouldAnimateFavoriteIcon = MutableStateFlow(false)
    override val shouldAnimateFavoriteIcon: StateFlow<Boolean> = _shouldAnimateFavoriteIcon
    private var mediaPlayer: MediaPlayer? = null
    private val logger = LoggerFactory.getLogger("ConnectionViewmodel")
    private val _hasPinnedIp = MutableStateFlow(false)
    override val hasPinnedIp: StateFlow<Boolean> = _hasPinnedIp
    private val _favouriteIconAnimation = MutableStateFlow(0)
    override val favouriteIconAnimation: StateFlow<Int> = _favouriteIconAnimation


    init {
        fetchLastLocation()
        fetchIPState()
        fetchNetworkState()
        fetchBestLocation()
        fetchUserPreferences()
        handleConnectionSoundsState()
        handleConnectionHapticFeedback()
        observeCustomLocationNameChanges()
        observeDecoyTrafficChanges()
        observePinnedIpChanges()
    }

    private fun fetchNewsfeedCount() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val notifications = localdb.getWindNotifications()
                var count = 0
                for ((notificationId) in notifications) {
                    if (!preferences.isNotificationAlreadyShown(notificationId.toString())) {
                        count++
                    }
                }
                _newFeedCount.emit(count)
            } catch (_ : Exception) {
                _newFeedCount.emit(0)
            }
        }
    }

    private fun fetchUserPreferences() {
        viewModelScope.launch {
            preferenceChangeListener = OnTrayPreferenceChangeListener {
                _isAntiCensorshipEnabled.value = preferences.isAntiCensorshipOn
                _aspectRatio.value = preferences.backgroundAspectRatioOption
                fetchNewsfeedCount()
            }
            preferences.addObserver(preferenceChangeListener!!)
        }
    }

    private fun fetchLastLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            val location = Util.getLastSelectedLocation(appContext)
            if (location != null) {
                lastLocationState.emit(location)
                fetchConnectionState()
            } else {
                try {
                    val bestCityAndRegion = locationRepository.getBestLocationAsync()
                    saveLastLocation(bestCityAndRegion)
                    _bestLocation.emit(ServerListItem(bestCityAndRegion.region.id, bestCityAndRegion.region, listOf(bestCityAndRegion.city)))
                    fetchConnectionState()
                } catch (_ : Exception) {
                    lastLocationState.value = null
                    fetchConnectionState()
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeCustomLocationNameChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                serverListRepository.customRegions,
                serverListRepository.customCities
            ) { _, _ -> }
                .debounce(100)
                .collectLatest {
                    fetchLastLocation()
                }
        }
    }

    private fun observeDecoyTrafficChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            decoyTrafficController.state.collect {
                _isDecoyTrafficEnabled.value = it
            }
        }
    }

    private fun saveLastLocation(location: CityAndRegion) {
        val coordinatesArray = location.city.coordinates.split(",".toRegex()).toTypedArray()
        val lastLocation = LastSelectedLocation(
            location.city.id,
            location.city.nodeName,
            location.city.nickName,
            location.region.countryCode,
            coordinatesArray[0],
            coordinatesArray[1]
        )
        Util.saveSelectedLocation(lastLocation)
        locationRepository.setSelectedCity(lastLocation.cityId)
        lastLocationState.value = lastLocation
    }

    private fun saveLastLocation(staticRegion: StaticRegion) {
        val lastLocation = LastSelectedLocation(
            staticRegion.id,
            staticRegion.cityName,
            staticRegion.staticIp,
            staticRegion.countryCode,
            "",
            ""
        )
        Util.saveSelectedLocation(lastLocation)
        locationRepository.setSelectedCity(lastLocation.cityId)
        lastLocationState.value = lastLocation
    }

    private fun saveLastLocation(config: ConfigFile) {
        val lastLocation =
            LastSelectedLocation(config.getPrimaryKey(), "Custom Config", config.name, "", "", "")
        Util.saveSelectedLocation(lastLocation)
        locationRepository.setSelectedCity(lastLocation.cityId)
        lastLocationState.value = lastLocation
    }

    private fun fetchNetworkState() {
        networkListener = object : NetworkInfoListener {
            override fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean) {
                if (networkInfo == null) {
                    _isPreferredProtocolEnabled.value = false
                    _networkInfoState.value = NetworkInfoState.Unknown
                } else {
                    logger.info("NetworkInfo: $networkInfo")
                    val protocolInfo = connectionUIState.value.protocolInfo
                    _isPreferredProtocolEnabled.value =
                        networkInfo.isPreferredOn && networkInfo.protocol == protocolInfo?.protocol && networkInfo.port == protocolInfo?.port
                    if (networkInfo.isAutoSecureOn) {
                        _networkInfoState.value = NetworkInfoState.Secured(networkInfo.networkName)
                    } else {
                        _networkInfoState.value =
                            NetworkInfoState.Unsecured(networkInfo.networkName)
                    }
                }
            }
        }
        networkInfoManager.addNetworkInfoListener(networkListener!!)
        networkInfoManager.reload(true)
    }

    private fun fetchIPState() {
        viewModelScope.launch(Dispatchers.IO) {
            ipRepository.state.collect { state ->
                when (state) {
                    is RepositoryState.Loading,
                    is RepositoryState.Error -> {
                        _ipState.emit("--.--.--.--")
                        _shouldAnimateIp.emit(false)
                    }

                    is RepositoryState.Success -> {
                        val newIp = state.data
                        val isValid = !newIp.contains("--")
                        val shouldAnimate = isValid && lastValidIp != null && lastValidIp != newIp
                        _shouldAnimateIp.emit(shouldAnimate)
                        _ipState.emit(newIp)
                        if (isValid) {
                            lastValidIp = newIp
                        }
                    }
                }
            }
        }
        ipRepository.update()
    }

    private fun fetchConnectionState() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                vpnConnectionStateManager.state,
                autoConnectionManager.connectedProtocol,
                autoConnectionManager.nextInLineProtocol
            ) { state, connectedProtocol, nextInLineProtocol ->
                val protocolInfo = when (state.status) {
                    VPNState.Status.Connected, VPNState.Status.Connecting -> connectedProtocol
                    else -> nextInLineProtocol
                } ?: Util.getAppSupportedProtocolList().first()
                val locationInfo = buildLocationInfo()
                when (state.status) {
                    VPNState.Status.Connected -> ConnectionUIState.Connected(
                        protocolInfo,
                        locationInfo,
                        preferences.lastConnectedUsingSplit
                    )

                    VPNState.Status.Connecting -> ConnectionUIState.Connecting(
                        protocolInfo,
                        locationInfo
                    )

                    VPNState.Status.Disconnected -> {
                        handleError(state)
                        ConnectionUIState.Disconnected(protocolInfo, locationInfo)
                    }

                    else -> ConnectionUIState.Disconnected(protocolInfo, locationInfo)
                }
            }.collectLatest { uiState ->
                _connectionUIState.value = uiState
            }
        }
    }

    private fun handleError(state: VPNState) {
        viewModelScope.launch(Dispatchers.IO) {
            if (state.error?.showError == true && state.error?.message != null) {
                _toastMessage.emit(ToastMessage.Raw(state.error!!.message))
            }
        }
    }

    private fun handleConnectionSoundsState() {
        viewModelScope.launch(Dispatchers.IO) {
            vpnConnectionStateManager.state.collectLatest {
                if (it.status == VPNState.Status.Connecting && preferences.connectedBundleSoundOption == 3) {
                    playSoundFromRaw(com.windscribe.vpn.R.raw.fart_deluxe_loop, loop = true)
                    return@collectLatest
                }
                if (it.status == VPNState.Status.Connected || it.status == VPNState.Status.Disconnected) {
                    val isConnected = vpnConnectionStateManager.isVPNConnected()
                    val option = if (isConnected) {
                        preferences.whenConnectedSoundOption
                    } else {
                        preferences.whenDisconnectedSoundOption
                    }
                    if (option == 1) {
                        return@collectLatest
                    }
                    if (option == 2) {
                        val index =
                            if (isConnected) preferences.connectedBundleSoundOption else preferences.disconnectedBundleSoundOption
                        val bundledSoundToPlay =
                            LookAndFeelHelper.getBundleSoundResource(isConnected, index)
                        bundledSoundToPlay?.let { sound ->
                            playSoundFromRaw(sound)
                        }
                    }
                    if (option == 3) {
                        val fileName = if (isConnected) {
                            preferences.customConnectedSound
                        } else {
                            preferences.customDisconnectedSound
                        }
                        if (fileName.isNullOrEmpty()) {
                            return@collectLatest
                        }
                        val inputFile = getSoundFile(appContext, isConnected, fileName)
                        if (inputFile.exists()) {
                            playSoundFromFile(inputFile.path)
                        }
                    }
                }
            }
        }
    }

    private fun playSoundFromFile(filePath: String, loop: Boolean = false) {
        try {
            mediaPlayer?.release() // release old one if exists
            mediaPlayer = MediaPlayer().apply {
                isLooping = loop
                setDataSource(filePath)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            logger.error("Error playing sound file at: $filePath", e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun playSoundFromRaw(@RawRes resId: Int, loop: Boolean = false) {
        val fileDescriptor = appContext.resources.openRawResourceFd(resId) ?: return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                isLooping = loop
                setDataSource(
                    fileDescriptor.fileDescriptor,
                    fileDescriptor.startOffset,
                    fileDescriptor.length
                )
                fileDescriptor.close()
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            logger.error("Error playing raw sound resource: $resId", e)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun handleConnectionHapticFeedback() {
        viewModelScope.launch(Dispatchers.IO) {
            vpnConnectionStateManager.state.collectLatest { state ->
                if (state.status == VPNState.Status.Connected || state.status == VPNState.Status.Disconnected) {
                    if (preferences.isHapticFeedbackEnabled) {
                        _shouldPlayHapticFeedback.emit(true)
                    }
                }
            }
        }
    }

    private fun buildLocationInfo(): LocationInfoState {
        var location = lastLocationState.value
        if (location == null || location.cityId != locationRepository.selectedCity.value) {
            location = Util.getLastSelectedLocation(appContext)
            lastLocationState.value = location
        }
        if (location == null) return LocationInfoState.Unavailable
        val cityId = location.cityId
        val countryCode = location.countryCode.orEmpty()
        return LocationInfoState.Success(
            LocationInfo(
                countryCode,
                serverListRepository.getCustomCityName(cityId) ?: location.nodeName,
                serverListRepository.getCustomCityNickName(cityId) ?: location.nickName,
                getLocationBackground(countryCode)
            )
        )
    }

    private fun getLocationBackground(countryCode: String): LocationBackground {
        val isConnected = vpnConnectionStateManager.isVPNConnected()
        val option = if (isConnected) {
            preferences.whenConnectedBackgroundOption
        } else {
            preferences.whenDisconnectedBackgroundOption
        }
        return when (option) {
            1 -> LocationBackground.Flag(FlagIconResource.getFlag(countryCode))
            2 -> {
                val index =
                    if (isConnected) preferences.connectedBundleBackgroundOption else preferences.disconnectedBundleBackgroundOption
                LocationBackground.Wallpaper(
                    bundledBackgrounds[index] ?: com.windscribe.vpn.R.mipmap.square
                )
            }

            3 -> LocationBackground.Flag(R.drawable.dummy_flag)
            else -> {
                val path = if (isConnected) {
                    preferences.customConnectedBackground
                } else {
                    preferences.customDisconnectedBackground
                }
                val image = if (isConnected) {
                    "connected_background.png"
                } else {
                    "disconnected_background.png"
                }
                if (path.isNullOrEmpty()) {
                    LocationBackground.Flag(R.drawable.dummy_flag)
                } else {
                    LocationBackground.Custom(File(appContext.filesDir, image))
                }
            }
        }
    }

    override fun onConnectButtonClick() {
        val currentState = connectionUIState.value
        val selectedLocation = lastLocationState.value ?: return
        if (currentState !is ConnectionUIState.Disconnected) {
            preferences.globalUserConnectionPreference = false
            vpnController.disconnectAsync()
            return
        }
        val locationSource = WindUtilities.getSourceTypeBlocking()
        appScope.launch {
            when (locationSource) {
                SelectedLocationType.CustomConfiguredProfile -> {
                    try {
                        val location = localdb.getConfigFileAsync(selectedLocation.cityId)
                        onConfigClick(location)
                    } catch (e: Exception) {
                        showToast("Unable to find selected location in database. Update server list.")
                    }
                }

                SelectedLocationType.StaticIp -> {
                    val staticIp = localdb.getStaticRegionByIDAsync(selectedLocation.cityId)
                    if (staticIp != null) {
                        onStaticIpClick(staticIp)
                    }
                }

                SelectedLocationType.CityLocation -> {
                    try {
                        val location = localdb.getCityAndRegion(selectedLocation.cityId)
                        onCityClick(location.city)
                    } catch (e: Exception) {
                        showToast("Unable to find selected location in database. Update server list.")
                    }
                }
            }
        }
    }

    override fun onCityClick(city: City) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isPro = userRepository.user.value?.isPro ?: false
                if (!city.isEnabled(isPro)) {
                    _goto.emit(HomeGoto.LocationMaintenance)
                    return@launch
                }
                val cityAndRegion = localdb.getCityAndRegion(city.id)
                val serverStatus = cityAndRegion.region.status
                val eligibleToConnect =
                    checkEligibility(cityAndRegion.city.pro, false, serverStatus)
                if (eligibleToConnect) {
                    preferences.globalUserConnectionPreference = true
                    preferences.setConnectingToStaticIP(false)
                    preferences.setConnectingToConfiguredLocation(false)
                    saveLastLocation(cityAndRegion)
                    logger.debug("Attempting to connect")
                    appScope.launch {
                        autoConnectionManager.connectInForeground()
                    }
                } else {
                    logger.info("User can not connect to city location.")
                }

            } catch (e: Exception) {
                showToast("Unable to find selected location in database. Update server list.")
            }
        }
    }

    override fun onStaticIpClick(staticRegion: StaticRegion) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val eligibleToConnect = checkEligibility(1, true, 1)
                if (eligibleToConnect) {
                    preferences.globalUserConnectionPreference = true
                    preferences.setConnectingToStaticIP(true)
                    preferences.setConnectingToConfiguredLocation(false)
                    saveLastLocation(staticRegion)
                    logger.debug("Attempting to connect..")
                    appScope.launch {
                        autoConnectionManager.connectInForeground()
                    }
                } else {
                    logger.info("User can not connect to location right now.")
                }

            } catch (e: Exception) {
                showToast("Unable to find selected location in database. Update server list.")
            }
        }
    }

    override fun onConfigClick(config: ConfigFile) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                locationRepository.setSelectedCity(config.getPrimaryKey())
                saveLastLocation(config)
                preferences.globalUserConnectionPreference = true
                preferences.setConnectingToConfiguredLocation(true)
                preferences.setConnectingToStaticIP(false)
                val type = WindUtilities.getConfigType(config.content)
                if (type == WindUtilities.ConfigType.OpenVPN && (config.username.isNullOrEmpty() || config.password.isNullOrEmpty())) {
                    _goto.emit(HomeGoto.EditCustomConfig(config.getPrimaryKey(), true))
                    return@launch
                } else {
                    vpnController.connectAsync()
                }
            } catch (e: Exception) {
                showToast("Unable to find selected location in database. Update server list.")
            }
        }
    }

    override fun onProtocolChangeClick() {
        if (WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CustomConfiguredProfile) {
            showToast(com.windscribe.vpn.R.string.protocol_change_is_not_available_for_custom_config)
        } else {
            appScope.launch {
                preferences.globalUserConnectionPreference = true
                autoConnectionManager.changeProtocolInForeground()
            }
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(ToastMessage.Raw(message))
        }
    }

    private fun showToast(@StringRes message: Int) {
        viewModelScope.launch {
            _toastMessage.emit(ToastMessage.Localized(message))
        }
    }

    override fun clearToast() {
        _toastMessage.value = ToastMessage.None
    }

    override fun onPinIPClick() {
        viewModelScope.launch(Dispatchers.IO) {
            setContextMenuState(false)
            val selectedCity = locationRepository.selectedCity.value
            val currentlyPinned = _hasPinnedIp.value
            val result = performPinIpAction(selectedCity, currentlyPinned)
            if (result) {
                _shouldAnimateFavoriteIcon.emit(true)
                val message = if (currentlyPinned) "IP unpinned successfully" else "IP pinned successfully"
                showToast(message)
            } else {
                val errorMessage = if (currentlyPinned) "IP Unpinning failed" else "IP Pinning failed"
                _goto.emit(HomeGoto.IpActionError(errorMessage))
            }
        }
    }

    override fun onRotateIpClick() {
        viewModelScope.launch {
            setContextMenuState(false)
            val result = performRotateIpAction()
            if (result) {
                showToast("IP rotated successfully")
            } else {
                _goto.emit(HomeGoto.IpActionError("IP Rotation failed"))
            }
        }
    }

    private suspend fun performPinIpAction(selectedCity: Int, currentlyPinned: Boolean): Boolean {
        return if (currentlyPinned) {
            unpinIp(selectedCity)
        } else {
            pinIp(selectedCity)
        }
    }

    /**
     * Pins the current VPN IP address to the selected city location.
     * Sends the current IP to the API and stores it in the local database as a favourite with pinned IP.
     *
     * @param selectedCity The city ID to pin the IP for
     * @return True if the pin operation was successful, false otherwise
     */
    private suspend fun pinIp(selectedCity: Int): Boolean {
        val ip = _ipState.value
        return when (val result = result<String> { api.pinIp(ip) }) {
            is CallResult.Success -> {
                try {
                    val city = localdb.getCityAndRegion(selectedCity)
                    val nodeIp = preferences.selectedIp
                    localdb.addToFavouritesAsync(Favourite(city.city.id, ip, nodeIp))
                    _favouriteIconAnimation.value = _favouriteIconAnimation.value + 1
                    logger.info("Pin IP request successful: ${result.data} $ip with nodeIp: $nodeIp")
                    true
                } catch (e: Exception) {
                    logger.error("Failed to save pinned IP to database", e)
                    false
                }
            }
            is CallResult.Error -> {
                logger.error("Pin IP request failed: ${result.errorMessage}")
                false
            }
        }.also { delay(300) }
    }

    /**
     * Unpins the IP address for the selected city location.
     * Sets the pinnedIp and pinnedNodeIp to null in the Favourite entry.
     * @param selectedCity The city ID to unpin the IP for
     * @return True if the unpin operation was successful, false otherwise
     */
    private suspend fun unpinIp(selectedCity: Int): Boolean {
        return try {
            localdb.addToFavouritesAsync(Favourite(selectedCity, null, null))
            true
        } catch (e: Exception) {
            logger.error("Failed to remove pinned IP from database", e)
            false
        }.also { delay(300) }
    }

    private suspend fun performRotateIpAction(): Boolean {
        return when (val result = result<String> { api.rotateIp() }) {
            is CallResult.Success -> {
                logger.info("Rotate IP request successful: ${result.data}")
                ipRepository.update()

                // Update pinned IP if the current city has one
                val selectedCity = locationRepository.selectedCity.value
                val favourites = localdb.getFavouritesAsync()
                val favourite = favourites.find { it.id == selectedCity && it.pinnedIp != null }
                if (favourite != null) {
                    // Wait for the IP to update before getting the new value
                    delay(500)
                    val newIp = _ipState.value
                    val nodeIp = preferences.selectedIp
                    localdb.addToFavouritesAsync(Favourite(selectedCity, newIp, nodeIp))
                    logger.info("Updated pinned IP after rotation: $newIp with nodeIp: $nodeIp")
                }

                true
            }
            is CallResult.Error -> {
                logger.error("Rotate IP request failed: ${result.errorMessage}")
                false
            }
        }.also { delay(300) }
    }

    private fun checkEligibility(isPro: Int, isStaticIp: Boolean, serverStatus: Int): Boolean {
        // Check Internet
        if (!WindUtilities.isOnline()) {
            logger.info("Error: no internet available.")
            showToast(com.windscribe.vpn.R.string.no_internet)
            return false
        }
        // User account status
        val user = userRepository.user.value
        if (user?.accountStatus == User.AccountStatus.Expired && !isStaticIp) {
            logger.info("Error: account status is expired.")
            val resetDate = user.nextResetDate() ?: ""
            viewModelScope.launch {
                _goto.emit(HomeGoto.Expired(resetDate))
            }
            return false
        }
        if (user?.accountStatus == User.AccountStatus.Banned) {
            logger.info("Error: account status is banned.")
            viewModelScope.launch {
                _goto.emit(HomeGoto.Banned)
            }
            return false
        }
        // Does user own this location
        if (preferences.userStatus != UserStatusConstants.USER_STATUS_PREMIUM && isPro == 1 && !isStaticIp) {
            logger.info("Location is pro but user is not. Opening upgrade activity.")
            viewModelScope.launch {
                _goto.emit(HomeGoto.Upgrade)
            }
            return false
        }
        // Set Static status
        preferences.setConnectingToStaticIP(isStaticIp)
        preferences.setConnectingToConfiguredLocation(false)
        // Check Network security
        val networkInfo = _networkInfoState.value
        if (networkInfo is NetworkInfoState.Unsecured && preferences.whiteListedNetwork != null) {
            preferences.whiteListedNetwork = networkInfo.name
        }
        if (serverStatus == NetworkKeyConstants.SERVER_STATUS_TEMPORARILY_UNAVAILABLE) {
            logger.info("Error: Server is temporary unavailable.")
            showToast("Location temporary unavailable.")
            viewModelScope.launch {
                _goto.emit(HomeGoto.LocationMaintenance)
            }
            return false
        }
        return true
    }

    override fun onIpContextMenuPosition(position: Offset) {
        viewModelScope.launch {
            val ipContextMenuState = _ipContextMenuState.value
            _ipContextMenuState.emit(Pair(ipContextMenuState.first, position))
        }
    }

    override fun setContextMenuState(state: Boolean) {
        viewModelScope.launch {
            val ipContextMenuState = _ipContextMenuState.value
            _ipContextMenuState.emit(Pair(state, ipContextMenuState.second))
        }
    }

    private fun fetchBestLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bestCityAndRegion = locationRepository.getBestLocationAsync()
                try {
                    _bestLocation.emit(ServerListItem(bestCityAndRegion.region.id, bestCityAndRegion.region, listOf(bestCityAndRegion.city)))
                }catch (ignored: Exception) {
                    _bestLocation.emit(null)
                }
            } catch (_ : Exception) {
                _bestLocation.emit(null)
            }
        }
    }

    override fun onGoToHandled() {
        viewModelScope.launch {
            _goto.emit(HomeGoto.None)
            _toastMessage.emit(ToastMessage.None)
        }
    }

    override fun setIsSingleLineLocationName(singleLine: Boolean) {
        viewModelScope.launch {
            _isSingleLineLocationName.emit(singleLine)
        }
    }

    override fun onHapticFeedbackHandled() {
        viewModelScope.launch {
            _shouldPlayHapticFeedback.emit(false)
        }
    }

    override fun onFavoriteAnimationHandled() {
        viewModelScope.launch {
            _shouldAnimateFavoriteIcon.emit(false)
        }
    }

    override fun onIpAnimationComplete() {
        viewModelScope.launch {
            _shouldAnimateIp.emit(false)
        }
    }

    private fun observePinnedIpChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            localdb.getFavourites().collectLatest { favourites ->
                val selectedCity = locationRepository.selectedCity.value
                val hasPinned = favourites.any { it.id == selectedCity && it.pinnedIp != null }
                _hasPinnedIp.emit(hasPinned)
            }
        }
    }

    override fun onCleared() {
        networkInfoManager.removeNetworkInfoListener(networkListener!!)
        preferences.removeObserver(preferenceChangeListener!!)
        viewModelScope.launch {
            _goto.emit(HomeGoto.None)
        }
        super.onCleared()
    }
}