package com.windscribe.mobile.ui.preferences.network_details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.repository.PortMapRepository
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.preferences.connection.PortMapItem
import com.windscribe.mobile.ui.preferences.connection.ProtoItem
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

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


class NetworkDetailViewModelImpl(
    val localDbInterface: LocalDbInterface,
    val api: IApiCallManager,
    val preferencesHelper: PreferencesHelper,
    val networkNetworkManager: NetworkInfoManager,
    val vpnController: WindVpnController,
    val vpnConnectionStateManager: VPNConnectionStateManager,
    val portMapRepository: PortMapRepository
) :
    NetworkDetailViewModel() {
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
        logger.info("Setting network name: $networkName")
        loadNetworkDetails()
    }

    private fun loadPortMapItems() {
        viewModelScope.launch(Dispatchers.IO) {
            logger.info("Loading portmap.")
            val portMapResult = portMapRepository.getPortMap()
            if (portMapResult.isSuccess) {
                val portMap = portMapResult.getOrNull()
                portMap?.portmap?.map {
                    PortMapItem(ProtoItem(it.protocol, it.heading), it.ports, it.use)
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
        portMapItems.map {
            DropDownStringItem(it.protoItem.proto, it.protoItem.heading)
        }.let {
            _protocols.value = it
            _selectedProtocol.value = networkDetail.value?.protocol ?: ""
            _selectedPort.value = networkDetail.value?.port ?: ""
            loadPorts()
        }
    }

    private fun loadPorts() {
        logger.info("Loading ports.")
        portMapItems.firstOrNull { item ->
            item.protoItem.proto == networkDetail.value?.protocol
        }?.ports?.let { ports ->
            _ports.value = ports.map { port -> DropDownStringItem(port, port) }
        }?:run {
            logger.error("Portmap item not found for protocol ${networkDetail.value?.protocol}")
        }
    }

    private fun loadNetworkDetails() {
        logger.info("Loading network details for network: $selectedNetworkName")
        viewModelScope.launch(Dispatchers.IO) {
            _showProgress.value = true
            localDbInterface.allNetworks
                .distinctUntilChanged()
                .collect { networkList ->
                    logger.debug("Received network list with ${networkList.size} networks")
                    val networkInfo = networkList.find { it.networkName == selectedNetworkName}
                    logger.debug("Found network info: ${networkInfo?.networkName}, protocol: ${networkInfo?.protocol}, port: ${networkInfo?.port}")
                    
                    if (_networkDetail.value != networkInfo) {
                        logger.info("Network detail changed, updating to: ${networkInfo?.networkName}")
                        _networkDetail.value = networkInfo
                    }
                    
                    val isMine = networkInfo?.networkName == networkNetworkManager.networkInfo.value?.networkName
                    if (isMyNetwork.value != isMine) {
                        logger.debug("isMyNetwork changed to: $isMine")
                        isMyNetwork.value = isMine
                    }
                    
                    val vpnActive = vpnConnectionStateManager.isVPNActive()
                    logger.debug("VPN active: $vpnActive, auto-secure: ${networkInfo?.isAutoSecureOn}, global pref: ${preferencesHelper.globalUserConnectionPreference}")
                    
                    when {
                        networkInfo?.isAutoSecureOn == true && !vpnActive && preferencesHelper.globalUserConnectionPreference -> {
                            logger.info("Auto-connecting VPN for secure network: ${networkInfo.networkName}")
                            vpnController.connectAsync()
                        }
                        networkInfo?.isAutoSecureOn == false && vpnActive && isMine -> {
                            logger.info("Auto-disconnecting VPN for non-secure network: ${networkInfo.networkName}")
                            vpnController.disconnectAsync()
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

    override fun onAutoSecureChanged() {
        viewModelScope.launch {
            _showProgress.value = true
            val networkInfo = _networkDetail.value
            networkInfo?.let {
                val updatedAutoSecure = !it.isAutoSecureOn
                it.isAutoSecureOn = updatedAutoSecure
                withContext(Dispatchers.IO) {
                    Log.i("NetworkDetail", "onAutoSecureChanged: $it")
                    localDbInterface.updateNetworkSync(it)
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
                val updatedPreferred = !it.isPreferredOn
                it.isPreferredOn = updatedPreferred
                withContext(Dispatchers.IO) {
                    localDbInterface.updateNetworkSync(it)
                }
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
                it.protocol = protocol.key
                
                // Always update ports list when protocol changes
                val portMapItem = portMapItems.firstOrNull { item -> item.protoItem.proto == protocol.key }
                val newPorts = portMapItem?.ports ?: emptyList()
                _ports.value = newPorts.map { port -> DropDownStringItem(port, port) }
                
                // If current port is not valid for new protocol, select first available port
                if (!isValidPort(it.protocol, it.port) && newPorts.isNotEmpty()) {
                    it.port = newPorts.first()
                    _selectedPort.value = it.port
                }
                
                withContext(Dispatchers.IO) {
                    localDbInterface.updateNetworkSync(it)
                }
            }
            _showProgress.value = false
        }
    }

    private fun isValidPort(protocol: String, port: String): Boolean {
        return portMapItems.firstOrNull {
            it.protoItem.proto == protocol
        }?.ports?.contains(port) ?: return false
    }

    override fun onPortSelected(port: DropDownStringItem) {
        viewModelScope.launch {
            _showProgress.value = true
            val networkInfo = _networkDetail.value
            networkInfo?.let {
                it.port = port.key
                withContext(Dispatchers.IO) {
                    localDbInterface.updateNetworkSync(it)
                }
            }
            _showProgress.value = false
        }
    }

    override fun onCleared() {
        networkNetworkManager.reload()
        super.onCleared()
    }
}