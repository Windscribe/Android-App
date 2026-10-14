package com.windscribe.mobile.ui.preferences.network_details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.preferences.connection.PortMapItem
import com.windscribe.mobile.ui.preferences.connection.ProtoItem
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.backend.utils.SYSTEM_DISCONNECT
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.PortMapRepository
import com.windscribe.vpn.state.ConnectionPolicy
import com.windscribe.vpn.state.ConnectionPolicyInputs
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.NetworkInfoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.inject.Inject

abstract class NetworkDetailViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val networkDetail: StateFlow<NetworkInfo?>
    abstract val selectedProtocol: StateFlow<String>
    abstract val selectedPort: StateFlow<String>
    abstract val protocols: StateFlow<List<DropDownStringItem>>
    abstract val ports: StateFlow<List<DropDownStringItem>>

    abstract fun setNetworkName(networkName: String)

    abstract fun onAutoSecureChanged()

    abstract fun onProtocolSelected(protocol: DropDownStringItem)

    abstract fun onPortSelected(port: DropDownStringItem)

    abstract fun forgetNetwork()

    abstract fun onPreferredChanged()

    abstract val isMyNetwork: StateFlow<Boolean>
}

@HiltViewModel
class NetworkDetailViewModelImpl
    @Inject
    constructor(
        val localDbInterface: LocalDbInterface,
        val api: IApiCallManager,
        val preferencesHelper: PreferencesHelper,
        val networkNetworkManager: NetworkInfoManager,
        val vpnController: WindVpnController,
        val portMapRepository: PortMapRepository,
        private val policyInputs: ConnectionPolicyInputs,
        private val deviceStateManager: DeviceStateManager,
    ) : NetworkDetailViewModel() {
        private val _showProgress = MutableStateFlow(false)
        override val showProgress: StateFlow<Boolean> = _showProgress
        private val _networkDetail = MutableStateFlow<NetworkInfo?>(null)
        override val networkDetail: StateFlow<NetworkInfo?> = _networkDetail
        private var selectedNetworkName: String? = null
        private val _selectedProtocol = MutableStateFlow("")
        override val selectedProtocol: StateFlow<String> = _selectedProtocol
        private val _selectedPort = MutableStateFlow("")
        override val selectedPort: StateFlow<String> = _selectedPort
        private val _protocols = MutableStateFlow(emptyList<DropDownStringItem>())
        override val protocols: StateFlow<List<DropDownStringItem>> = _protocols
        private val _ports = MutableStateFlow(emptyList<DropDownStringItem>())
        override val ports: StateFlow<List<DropDownStringItem>> = _ports
        private val portMapItems = mutableListOf<PortMapItem>()
        override val isMyNetwork = MutableStateFlow(false)
        private val logger = LoggerFactory.getLogger("NetworkDetail")
        private var portMapLoaded = false

        override fun setNetworkName(networkName: String) {
            if (selectedNetworkName == networkName) {
                logger.debug("Network name already set to: $networkName, skipping")
                return
            }
            selectedNetworkName = networkName
            logger.debug("Setting network name: $networkName")
            loadNetworkDetails()
        }

        private fun loadPortMapItems() {
            viewModelScope.launch(Dispatchers.IO) {
                logger.debug("Loading portmap.")
                val portMapResult = portMapRepository.getPortMap()
                if (portMapResult.isSuccess) {
                    val portMap = portMapResult.getOrNull()
                    portMap
                        ?.portmap
                        ?.map {
                            PortMapItem(ProtoItem(it.protocol ?: "", it.heading ?: ""), it.ports ?: emptyList(), it.use ?: "")
                        }?.let {
                            portMapItems.addAll(it)
                            buildProtocolInfo()
                        }
                    if (portMap == null) {
                        logger.error("Portmap is null.")
                    }
                } else {
                    logger.error("Portmap loading failed. ${portMapResult.exceptionOrNull()?.message}")
                }
            }
        }

        private fun buildProtocolInfo() {
            portMapItems
                .map {
                    DropDownStringItem(it.protoItem.proto, it.protoItem.heading)
                }.let {
                    _protocols.value = it
                    _selectedProtocol.value = networkDetail.value?.protocol ?: ""
                    _selectedPort.value = networkDetail.value?.port ?: ""
                    loadPorts()
                }
        }

        private fun loadPorts() {
            logger.debug("Loading ports.")
            portMapItems
                .firstOrNull { item ->
                    item.protoItem.proto == networkDetail.value?.protocol
                }?.ports
                ?.let { ports ->
                    _ports.value = ports.map { port -> DropDownStringItem(port, port) }
                } ?: run {
                logger.error("Portmap item not found for protocol ${networkDetail.value?.protocol}")
            }
        }

        private fun loadNetworkDetails() {
            logger.debug("Loading network details for network: $selectedNetworkName")
            viewModelScope.launch(Dispatchers.IO) {
                _showProgress.value = true
                localDbInterface.allNetworks
                    .distinctUntilChanged()
                    .collect { networkList ->
                        logger.debug("Received network list with ${networkList.size} networks")
                        val networkInfo = networkList.find { it.networkName == selectedNetworkName }
                        logger.debug(
                            "Found network info: ${networkInfo?.networkName}, protocol: ${networkInfo?.protocol}, port: ${networkInfo?.port}",
                        )

                        if (_networkDetail.value != networkInfo) {
                            logger.debug("Network detail changed, updating to: ${networkInfo?.networkName}")
                            _networkDetail.value = networkInfo
                        }

                        val isMine = networkInfo?.networkName == networkNetworkManager.networkInfo.value?.networkName
                        if (isMyNetwork.value != isMine) {
                            logger.debug("isMyNetwork changed to: $isMine")
                            isMyNetwork.value = isMine
                        }

                        // Editing another saved network's settings must not connect or disconnect.
                        if (isMine) {
                            val input = policyInputs.from(networkInfo)
                            when {
                                ConnectionPolicy.shouldAutoConnect(input) -> {
                                    logger.info("Auto-connecting for secure network: ${networkInfo?.networkName}")
                                    vpnController.connectAsync(ConnectSource.App)
                                }

                                ConnectionPolicy.shouldAutoDisconnect(input) -> {
                                    logger.info("Auto-disconnecting for non-secure network: ${networkInfo?.networkName}")
                                    // Auto-Secure off already stops auto-connect here; standing the
                                    // network down as well would be redundant and misreported.
                                    vpnController.disconnectAsync(SYSTEM_DISCONNECT)
                                }
                            }
                        }

                        // Load port map only once after we have network details
                        if (!portMapLoaded) {
                            portMapLoaded = true
                            loadPortMapItems()
                        }

                        _showProgress.value = false
                    }
            }
        }

        /**
         * Publishes a changed row, writes it, then republishes the shared flow; all three matter.
         *
         * [NetworkInfo] is a value type, so the change must be a new instance — mutating the one the
         * flow holds makes it compare equal to the row read back and emit nothing. The reload is for
         * readers outside this screen, which take the network from `networkInfo.value`.
         */
        private suspend fun persist(updated: NetworkInfo) {
            _networkDetail.value = updated
            withContext(Dispatchers.IO) {
                localDbInterface.updateNetworkSync(updated)
            }
            networkNetworkManager.reload()
        }

        override fun onAutoSecureChanged() {
            viewModelScope.launch {
                _showProgress.value = true
                val networkInfo = _networkDetail.value
                networkInfo?.let {
                    val updatedAutoSecure = !it.isAutoSecureOn
                    // A copy, never a mutation of the value the flow is holding. Mutating it in place
                    // makes the row the database returns a moment later compare *equal* to what is
                    // already published, so nothing is emitted and the screen sits on stale state.
                    val updated = it.copy(isAutoSecureOn = updatedAutoSecure)
                    Log.i("NetworkDetail", "onAutoSecureChanged: $updated")
                    persist(updated)
                    // Turning Auto-Secure off by hand withdraws any earlier "connect here", so it
                    // applies now instead of being exempt for the rest of the session.
                    if (!updatedAutoSecure && isMyNetwork.value) {
                        deviceStateManager.setOptedInNetwork(null)
                    }
                }
                _showProgress.value = false
            }
        }

        override fun onPreferredChanged() {
            viewModelScope.launch {
                _showProgress.value = true
                val networkInfo = _networkDetail.value
                networkInfo?.let {
                    persist(it.copy(isPreferredOn = !it.isPreferredOn))
                }
                _showProgress.value = false
            }
        }

        override fun forgetNetwork() {
            viewModelScope.launch {
                _showProgress.value = true
                val networkInfo = _networkDetail.value
                networkInfo?.let {
                    withContext(Dispatchers.IO) {
                        localDbInterface.deleteNetworkSync(it.networkName)
                    }
                }
                _showProgress.value = false
            }
        }

        override fun onProtocolSelected(protocol: DropDownStringItem) {
            viewModelScope.launch {
                _showProgress.value = true
                val networkInfo = _networkDetail.value
                networkInfo?.let {
                    var updated = it.copy(protocol = protocol.key)

                    // Always update ports list when protocol changes
                    val portMapItem = portMapItems.firstOrNull { item -> item.protoItem.proto == protocol.key }
                    val newPorts = portMapItem?.ports ?: emptyList()
                    _ports.value = newPorts.map { port -> DropDownStringItem(port, port) }

                    // If current port is not valid for new protocol, select first available port
                    if (!isValidPort(updated.protocol ?: "", updated.port ?: "") && newPorts.isNotEmpty()) {
                        updated = updated.copy(port = newPorts.first())
                        _selectedPort.value = updated.port ?: ""
                    }

                    persist(updated)
                }
                _showProgress.value = false
            }
        }

        private fun isValidPort(
            protocol: String,
            port: String,
        ): Boolean {
            return portMapItems
                .firstOrNull {
                    it.protoItem.proto == protocol
                }?.ports
                ?.contains(port) ?: return false
        }

        override fun onPortSelected(port: DropDownStringItem) {
            viewModelScope.launch {
                _showProgress.value = true
                val networkInfo = _networkDetail.value
                networkInfo?.let {
                    persist(it.copy(port = port.key))
                }
                _showProgress.value = false
            }
        }

        override fun onCleared() {
            networkNetworkManager.reload()
            super.onCleared()
        }
    }
