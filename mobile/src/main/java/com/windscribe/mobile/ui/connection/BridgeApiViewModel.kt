package com.windscribe.mobile.ui.connection

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.BridgeApiRepository
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.IpRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.mobile.ui.home.HomeGoto
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.commonutils.WindUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import javax.inject.Inject

abstract class BridgeApiViewModel : ViewModel() {
    abstract val ipContextMenuState: StateFlow<Pair<Boolean, Offset>>
    abstract val hasPinnedIp: StateFlow<Boolean>
    abstract val favouriteIconAnimation: StateFlow<Int>
    abstract val bridgeApiReady: StateFlow<Boolean>
    abstract val ipState: StateFlow<String>
    abstract val goto: SharedFlow<HomeGoto>
    abstract val isRotatingIp: StateFlow<Boolean>

    abstract fun onIpContextMenuPosition(position: Offset)
    abstract fun setContextMenuState(state: Boolean)
    abstract fun onPinIPClick(onSuccess: (Boolean, String) -> Unit)
    abstract fun onRotateIpClick(onSuccess: (String) -> Unit)
    abstract fun onGoToHandled()
}

class BridgeApiViewModelImpl @Inject constructor(
    private val bridgeApiRepository: BridgeApiRepository,
    private val locationRepository: LocationRepository,
    private val localdb: LocalDbInterface,
    private val api: IApiCallManager,
    private val ipRepository: IpRepository,
    private val preferences: PreferencesHelper,
    private val resourceHelper: ResourceHelper
) : BridgeApiViewModel() {

    private val logger = LoggerFactory.getLogger("BridgeApiViewModel")

    private val _ipContextMenuState = MutableStateFlow(Pair(false, Offset.Zero))
    override val ipContextMenuState: StateFlow<Pair<Boolean, Offset>> = _ipContextMenuState

    private val _hasPinnedIp = MutableStateFlow(false)
    override val hasPinnedIp: StateFlow<Boolean> = _hasPinnedIp

    private val _favouriteIconAnimation = MutableStateFlow(0)
    override val favouriteIconAnimation: StateFlow<Int> = _favouriteIconAnimation

    private val _bridgeApiReady = MutableStateFlow(false)
    override val bridgeApiReady: StateFlow<Boolean> = _bridgeApiReady

    private val _ipState: MutableStateFlow<String> = MutableStateFlow("")
    override val ipState: StateFlow<String> = _ipState

    private val _goto = MutableSharedFlow<HomeGoto>(replay = 0)
    override val goto: SharedFlow<HomeGoto> = _goto

    private val _isRotatingIp = MutableStateFlow(false)
    override val isRotatingIp: StateFlow<Boolean> = _isRotatingIp

    init {
        observePinnedIpChanges()
        observeBridgeApi()
        observeIpState()
    }

    private fun observeIpState() {
        viewModelScope.launch(Dispatchers.IO) {
            ipRepository.state.collect { state ->
                when (state) {
                    is com.windscribe.vpn.repository.RepositoryState.Success -> {
                        _ipState.emit(state.data)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observePinnedIpChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                localdb.getFavourites(),
                locationRepository.selectedCity
            ) { favourites, selectedCityId ->
                val cityId = locationRepository.getSelectedCityAndRegion()?.city?.id ?: selectedCityId
                val hasPinned = favourites.any { it.id == cityId && it.pinnedIp != null }
                _hasPinnedIp.emit(hasPinned)
            }.collectLatest { }
        }
    }

    private fun observeBridgeApi() {
        viewModelScope.launch(Dispatchers.IO) {
            bridgeApiRepository.apiAvailable.collectLatest {
                _bridgeApiReady.emit(it)
                if (_ipContextMenuState.value.first && !it) {
                    setContextMenuState(false)
                }
            }
        }
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

    override fun onPinIPClick(onSuccess: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            setContextMenuState(false)
            val selectedCity = locationRepository.selectedCity.value
            val currentlyPinned = _hasPinnedIp.value
            val result = performPinIpAction(selectedCity, currentlyPinned)
            withContext(Dispatchers.Main) {
                if (result) {
                    val messageResId = if (currentlyPinned) com.windscribe.vpn.R.string.ip_unpinned_successfully else com.windscribe.vpn.R.string.ip_pinned_successfully
                    val message = resourceHelper.getString(messageResId)
                    onSuccess(currentlyPinned, message)
                } else {
                    val errorMessageResId = if (currentlyPinned) com.windscribe.vpn.R.string.could_not_unpin_ip else com.windscribe.vpn.R.string.could_not_pin_ip
                    val errorMessage = resourceHelper.getString(errorMessageResId)
                    val errorDescription = resourceHelper.getString(com.windscribe.vpn.R.string.favourite_node_not_available)
                    _goto.emit(HomeGoto.IpActionError(errorMessage, errorDescription))
                }
            }
        }
    }

    override fun onRotateIpClick(onSuccess: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            setContextMenuState(false)
            val result = performRotateIpAction()
            withContext(Dispatchers.Main) {
                if (result) {
                    val message = resourceHelper.getString(com.windscribe.vpn.R.string.ip_rotated_successfully)
                    onSuccess(message)
                } else {
                    val errorMessage = resourceHelper.getString(com.windscribe.vpn.R.string.could_not_rotate_ip)
                    val errorDescription = resourceHelper.getString(com.windscribe.vpn.R.string.check_status_description)
                    _goto.emit(HomeGoto.IpActionError(errorMessage, errorDescription))
                }
            }
        }
    }

    override fun onGoToHandled() {
        viewModelScope.launch {
            _goto.emit(HomeGoto.None)
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
     * @param selectedCity The city ID to pin the IP for
     * @return True if the pin operation was successful, false otherwise
     */
    private suspend fun pinIp(selectedCity: Int): Boolean {
        val ip = _ipState.value
        return when (val result = result<String> { api.pinIp(ip) }) {
            is CallResult.Success -> {
                try {
                    val city = localdb.getCityAndRegion(selectedCity)
                    if (city == null) {
                        logger.error("City not found in database: $selectedCity")
                        return false
                    }
                    val nodeIp = preferences.selectedIp
                    localdb.addToFavouritesAsync(Favourite(city.city.id, ip, nodeIp))
                    logger.info("Pin IP request successful: ${result.data} $ip with nodeIp: $nodeIp")
                    _favouriteIconAnimation.value = _favouriteIconAnimation.value + 1
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
        }
    }

    private suspend fun unpinIp(selectedCity: Int): Boolean {
        return try {
            localdb.deleteFavourite(selectedCity)
            true
        } catch (e: Exception) {
            logger.error("Failed to remove pinned IP from database", e)
            false
        }
    }

    private suspend fun performRotateIpAction(): Boolean {
        _isRotatingIp.emit(true)
        return try {
            when (val result = result<String> { api.rotateIp() }) {
                is CallResult.Success -> {
                    val currentIp = _ipState.value
                    ipRepository.update()

                    // Wait for IP repository to complete the update
                    val ipUpdated = withTimeoutOrNull(5000) {
                        _ipState.first { newIp ->
                            !newIp.contains("--") // Wait for valid IP (may be same as before)
                        }
                    } != null

                    val newIp = _ipState.value
                    if (ipUpdated) {
                        if (newIp != currentIp) {
                            logger.info("IP changed from $currentIp to $newIp")
                        } else {
                            logger.info("IP rotated successfully (same IP reassigned: $newIp)")
                        }
                    } else {
                        logger.warn("IP state update timed out, but rotation API succeeded")
                    }

                    // Trust the API response - rotation succeeded even if IP is the same
                    true
                }
                is CallResult.Error -> {
                    logger.error("Rotate IP request failed: ${result.errorMessage}")
                    false
                }
            }
        } finally {
            _isRotatingIp.emit(false)
        }
    }
}