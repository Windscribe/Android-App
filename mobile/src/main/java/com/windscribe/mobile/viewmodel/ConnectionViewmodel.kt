package com.windscribe.mobile.viewmodel

import android.media.MediaPlayer
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.R
import com.windscribe.mobile.lipstick.LookAndFeelHelper
import com.windscribe.mobile.lipstick.LookAndFeelHelper.bundledBackgrounds
import com.windscribe.mobile.lipstick.LookAndFeelHelper.getSoundFile
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.Ext.toResult
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.IpRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.RepositoryState
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.CityAndRegion
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.state.NetworkInfoListener
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

sealed class HomeGoto {
    object Upgrade : HomeGoto()
    data class Expired(val date: String) : HomeGoto()
    object Banned : HomeGoto()
    object None : HomeGoto()
}

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
        override val locationInfo: LocationInfoState
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
    abstract val networkInfoState: StateFlow<NetworkInfoState>
    abstract val ipContextMenuState: StateFlow<Pair<Boolean, Offset>>
    abstract val bestLocation: StateFlow<ServerListItem?>
    abstract val isAntiCensorshipEnabled: StateFlow<Boolean>
    abstract val isPreferredProtocolEnabled: StateFlow<Boolean>
    abstract val aspectRatio: StateFlow<Int>
    abstract val goto: StateFlow<HomeGoto>
    abstract val newFeedCount: StateFlow<Int>
    abstract fun onConnectButtonClick()
    abstract fun onCityClick(city: City)
    abstract fun onStaticIpClick(staticRegion: StaticRegion)
    abstract fun onConfigClick(config: ConfigFile)
    abstract fun onIpContextMenuPosition(position: Offset)
    abstract fun onRotateIpClick()
    abstract fun onFavouriteIpClick()
    abstract fun setContextMenuState(state: Boolean)
    abstract val toastMessage: StateFlow<ToastMessage>
    abstract fun clearToast()
    abstract fun clearGoTo()
    abstract fun onProtocolChangeClick()
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
    private val serverListRepository: ServerListRepository
) :
    ConnectionViewmodel() {
    private val _connectionUIState = MutableStateFlow<ConnectionUIState>(ConnectionUIState.Idle)
    override val connectionUIState: StateFlow<ConnectionUIState> = _connectionUIState

    private val _ipState: MutableStateFlow<String> = MutableStateFlow("")
    override val ipState: StateFlow<String> = _ipState
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
    private val _goto = MutableStateFlow<HomeGoto>(HomeGoto.None)
    override val goto: StateFlow<HomeGoto> = _goto
    private val _newFeedCount = MutableStateFlow(0)
    override val newFeedCount: StateFlow<Int> = _newFeedCount
    private var preferenceChangeListener: OnTrayPreferenceChangeListener? = null

    private val lastLocationState: MutableStateFlow<LastSelectedLocation?> = MutableStateFlow(null)
    private var networkListener: NetworkInfoListener? = null
    private val _aspectRatio = MutableStateFlow(1)
    override val aspectRatio: StateFlow<Int> = _aspectRatio
    private var mediaPlayer: MediaPlayer? = null
    private val logger = LoggerFactory.getLogger("ConnectionViewmodel")


    init {
        fetchLastLocation()
        fetchIPState()
        fetchNetworkState()
        fetchBestLocation()
        fetchUserPreferences()
        handleConnectionSoundsState()
    }

    private fun fetchNewsfeedCount() {
        viewModelScope.launch(Dispatchers.IO) {
            localdb.windNotifications.toResult().onSuccess {
                var count = 0
                for ((notificationId) in it) {
                    if (!preferences.isNotificationAlreadyShown(notificationId.toString())) {
                        count++
                    }
                }
                _newFeedCount.emit(count)
            }.onFailure {
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
                locationRepository.bestLocation.toResult()
                    .onSuccess {
                        saveLastLocation(it)
                        _bestLocation.emit(ServerListItem(it.region.id, it.region, listOf(it.city)))
                        fetchConnectionState()
                    }.onFailure {
                        lastLocationState.value = null
                        fetchConnectionState()
                    }
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
                    }

                    is RepositoryState.Success -> {
                        _ipState.emit(state.data)
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
                        locationInfo
                    )

                    VPNState.Status.Connecting -> ConnectionUIState.Connecting(
                        protocolInfo,
                        locationInfo
                    )

                    else -> ConnectionUIState.Disconnected(protocolInfo, locationInfo)
                }
            }.collectLatest { uiState ->
                _connectionUIState.value = uiState
            }
        }
    }

    private fun handleConnectionSoundsState() {
        viewModelScope.launch(Dispatchers.IO) {
            vpnConnectionStateManager.state.collectLatest {
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

    private fun playSoundFromFile(filePath: String) {
        try {
            mediaPlayer?.release() // release old one if exists
            mediaPlayer = MediaPlayer().apply {
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

    private fun playSoundFromRaw(@RawRes resId: Int) {
        val fileDescriptor = appContext.resources.openRawResourceFd(resId) ?: return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
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


    private fun buildLocationInfo(): LocationInfoState {
        val lastSelectedLocation = lastLocationState.value
        return if (lastSelectedLocation == null) {
            LocationInfoState.Unavailable
        } else {
            val countryCode = lastSelectedLocation.countryCode ?: ""
            LocationInfoState.Success(
                LocationInfo(
                    countryCode,
                    serverListRepository.getCustomCityName(lastSelectedLocation.cityId)
                        ?: lastSelectedLocation.nodeName,
                    serverListRepository.getCustomCityNickName(lastSelectedLocation.cityId)
                        ?: lastSelectedLocation.nickName,
                    getLocationBackground(countryCode)
                )
            )
        }
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
                LocationBackground.Wallpaper(bundledBackgrounds[index] ?: R.mipmap.square)
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
            vpnController.disconnectAsync()
            return
        }
        val locationSource = WindUtilities.getSourceTypeBlocking()
        appScope.launch {
            when (locationSource) {
                SelectedLocationType.CustomConfiguredProfile -> {
                    localdb.getConfigFile(selectedLocation.cityId).toResult()
                        .onSuccess { onConfigClick(it) }
                }

                SelectedLocationType.StaticIp -> {
                    localdb.getStaticRegionByID(selectedLocation.cityId).toResult()
                        .onSuccess { onStaticIpClick(it) }
                }

                SelectedLocationType.CityLocation -> {
                    val location = localdb.getCityAndRegion(selectedLocation.cityId)
                    onCityClick(location.city)
                }
            }
        }
    }

    override fun onCityClick(city: City) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                vpnController.connectAsync()
            } catch (e: Exception) {
                showToast("Unable to find selected location in database. Update server list.")
            }
        }
    }

    override fun onProtocolChangeClick() {
        if (WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CustomConfiguredProfile) {
            showToast(R.string.protocol_change_is_not_available_for_custom_config)
        } else if (vpnConnectionStateManager.isVPNConnected()) {
            appScope.launch {
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

    override fun onFavouriteIpClick() {
        setContextMenuState(false)
        showToast("Feature not available yet.")
    }

    override fun onRotateIpClick() {
        setContextMenuState(false)
        showToast("Feature not available yet.")
    }

    private fun checkEligibility(isPro: Int, isStaticIp: Boolean, serverStatus: Int): Boolean {
        // Check Internet
        if (!WindUtilities.isOnline()) {
            logger.info("Error: no internet available.")
            showToast(R.string.no_internet)
            return false
        }
        val user = userRepository.user.value
        // Does user own this location
        if (preferences.userStatus != UserStatusConstants.USER_STATUS_PREMIUM && isPro == 1 && !isStaticIp) {
            logger.info("Location is pro but user is not. Opening upgrade activity.")
            viewModelScope.launch {
                _goto.emit(HomeGoto.Upgrade)
            }
            return false
        }
        // User account status
        if (user?.accountStatusToInt == UserStatusConstants.ACCOUNT_STATUS_EXPIRED && !isStaticIp) {
            logger.info("Error: account status is expired.")
            val resetDate = user.nextResetDate() ?: ""
            viewModelScope.launch {
                _goto.emit(HomeGoto.Expired(resetDate))
            }
            return false
        }
        if (user?.userStatusInt == UserStatusConstants.ACCOUNT_STATUS_BANNED) {
            logger.info("Error: account status is banned.")
            viewModelScope.launch {
                _goto.emit(HomeGoto.Banned)
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
            locationRepository.bestLocation.toResult()
                .onSuccess {
                    _bestLocation.emit(ServerListItem(it.region.id, it.region, listOf(it.city)))
                }.onFailure {
                    _bestLocation.emit(null)
                }
        }
    }

    override fun clearGoTo() {
        viewModelScope.launch {
            _goto.emit(HomeGoto.None)
        }
    }

    override fun onCleared() {
        networkInfoManager.removeNetworkInfoListener(networkListener!!)
        preferences.removeObserver(preferenceChangeListener!!)
        super.onCleared()
    }
}

fun mockConnectionViewmodel(): ConnectionViewmodel {
    return object : ConnectionViewmodel() {
        override val connectionUIState: StateFlow<ConnectionUIState>
            get() = MutableStateFlow(ConnectionUIState.Idle)
        override val ipState: StateFlow<String>
            get() = MutableStateFlow("127.0.0.1")
        override val networkInfoState: StateFlow<NetworkInfoState>
            get() = MutableStateFlow(NetworkInfoState.Unknown)
        override val ipContextMenuState: StateFlow<Pair<Boolean, Offset>>
            get() = MutableStateFlow(Pair(false, Offset.Zero))
        override val toastMessage: StateFlow<ToastMessage>
            get() = MutableStateFlow(ToastMessage.None)
        override val bestLocation: StateFlow<ServerListItem?>
            get() = MutableStateFlow(null)
        override val isAntiCensorshipEnabled: StateFlow<Boolean>
            get() = MutableStateFlow(false)
        override val isPreferredProtocolEnabled: StateFlow<Boolean>
            get() = MutableStateFlow(false)
        override val goto: StateFlow<HomeGoto>
            get() = MutableStateFlow(HomeGoto.None)
        override val newFeedCount: StateFlow<Int>
            get() = MutableStateFlow(0)
        override val aspectRatio: StateFlow<Int>
            get() = MutableStateFlow(1)

        override fun onConnectButtonClick() {}
        override fun onCityClick(city: City) {}
        override fun onStaticIpClick(staticRegion: StaticRegion) {}
        override fun onConfigClick(config: ConfigFile) {}
        override fun onIpContextMenuPosition(position: Offset) {}
        override fun onRotateIpClick() {}
        override fun onFavouriteIpClick() {}
        override fun setContextMenuState(state: Boolean) {}
        override fun clearToast() {}
        override fun onProtocolChangeClick() {}
        override fun clearGoTo() {}
    }
}