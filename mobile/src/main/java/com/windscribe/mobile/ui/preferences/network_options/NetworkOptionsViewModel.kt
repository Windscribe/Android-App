package com.windscribe.mobile.ui.preferences.network_options

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.NetworkInfoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class NetworkOptionsViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val autoSecureEnabled: StateFlow<Boolean>
    abstract val currentNetwork: StateFlow<NetworkInfo?>
    abstract val allNetworks: StateFlow<List<NetworkInfo>>
    abstract fun onAutoSecureChanged()
}

class NetworkOptionsViewModelImpl(
    val preferenceHelper: PreferencesHelper,
    val networkInfoManager: NetworkInfoManager,
    val localDbInterface: LocalDbInterface
) : NetworkOptionsViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    private val _autoSecureEnabled = MutableStateFlow(preferenceHelper.isAutoSecureOn)
    override val autoSecureEnabled: StateFlow<Boolean> = _autoSecureEnabled
    private val _currentNetwork = MutableStateFlow<NetworkInfo?>(null)
    override val currentNetwork: StateFlow<NetworkInfo?> = _currentNetwork
    private val _allNetworks = MutableStateFlow<List<NetworkInfo>>(emptyList())
    override val allNetworks: StateFlow<List<NetworkInfo>> = _allNetworks

    init {
        loadNetworks()
    }

    private fun loadNetworks() {
        viewModelScope.launch(Dispatchers.IO) {
            localDbInterface.allNetworks.collectLatest {
                _showProgress.value = false
                if (it.isEmpty()) {
                    Log.i("NetworkOptionsViewModel", "loadNetworks: Empty")
                    _currentNetwork.value = null
                    _allNetworks.value = emptyList()
                } else {
                    Log.i("NetworkOptionsViewModel", "loadNetworks: $it")
                    val currentNetwork = networkInfoManager.networkInfo?.networkName
                    _allNetworks.value = it.filter { it.networkName != currentNetwork }
                    _currentNetwork.value = it.find { it.networkName == currentNetwork }
                }
            }
        }
    }

    override fun onAutoSecureChanged() {
        viewModelScope.launch {
            val updated = _autoSecureEnabled.value.not()
            _autoSecureEnabled.value = updated
            preferenceHelper.isAutoSecureOn = updated
        }
    }
}