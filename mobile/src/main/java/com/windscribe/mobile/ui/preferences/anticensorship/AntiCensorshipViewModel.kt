package com.windscribe.mobile.ui.preferences.anticensorship

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UnblockWgParamsRepository
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class AntiCensorshipViewModel : ViewModel() {
    abstract val protocolTweaksEnabled: StateFlow<Boolean>
    abstract val amneziaPresets: StateFlow<List<DropDownStringItem>>
    abstract val selectedPreset: StateFlow<String>
    abstract val serverRoutingModes: StateFlow<List<DropDownStringItem>>
    abstract val selectedServerRouting: StateFlow<String>
    abstract fun onProtocolTweaksToggled()
    abstract fun onAmneziaPresetSelected(presetId: String)
    abstract fun onServerRoutingSelected(mode: String)
    abstract fun refreshPreferences()
}

class AntiCensorshipViewModelImpl(
    private val preferencesHelper: PreferencesHelper,
    private val unblockWgParamsRepository: UnblockWgParamsRepository,
    private val serverListRepository: ServerListRepository
) : AntiCensorshipViewModel() {

    private val _protocolTweaksEnabled = MutableStateFlow(preferencesHelper.isProtocolTweaksEnabled)
    override val protocolTweaksEnabled: StateFlow<Boolean> = _protocolTweaksEnabled

    private val _amneziaPresets = MutableStateFlow(emptyList<DropDownStringItem>())
    override val amneziaPresets: StateFlow<List<DropDownStringItem>> = _amneziaPresets

    private val _selectedPreset = MutableStateFlow(
        unblockWgParamsRepository.getSelectedUnblockWgParam()?.id ?: ""
    )
    override val selectedPreset: StateFlow<String> = _selectedPreset

    private val _serverRoutingModes = MutableStateFlow(
        listOf(
            DropDownStringItem(PreferencesKeyConstants.SERVER_ROUTING_AUTO, "Auto"),
            DropDownStringItem(PreferencesKeyConstants.SERVER_ROUTING_REGULAR, "Regular"),
            DropDownStringItem(PreferencesKeyConstants.SERVER_ROUTING_ALTERNATE, "Alternate")
        )
    )
    override val serverRoutingModes: StateFlow<List<DropDownStringItem>> = _serverRoutingModes

    private val _selectedServerRouting = MutableStateFlow(preferencesHelper.serverRoutingMode)
    override val selectedServerRouting: StateFlow<String> = _selectedServerRouting

    init {
        loadAmneziaPresets()
    }

    private fun loadAmneziaPresets() {
        viewModelScope.launch {
            unblockWgParamsRepository.unblockWgParams.collectLatest { params ->
                val presets = params.map { DropDownStringItem(it.id, it.title) }
                _amneziaPresets.emit(presets)
                val selected = params.firstOrNull { it.id == _selectedPreset.value }
                if (selected == null && presets.isNotEmpty()) {
                    _selectedPreset.emit(presets.first().key)
                }
            }
        }
    }

    override fun onProtocolTweaksToggled() {
        viewModelScope.launch {
            val newValue = !_protocolTweaksEnabled.value
            _protocolTweaksEnabled.emit(newValue)
            preferencesHelper.isProtocolTweaksEnabled = newValue

            // Mark as manually configured
            if (!preferencesHelper.isAntiCensorshipManualMode) {
                preferencesHelper.isAntiCensorshipManualMode = true
            }
        }
    }

    override fun onAmneziaPresetSelected(presetId: String) {
        viewModelScope.launch {
            _selectedPreset.emit(presetId)
            unblockWgParamsRepository.setSelectedUnblockWgParam(presetId)

            // Mark as manually configured
            if (!preferencesHelper.isAntiCensorshipManualMode) {
                preferencesHelper.isAntiCensorshipManualMode = true
            }
        }
    }

    override fun onServerRoutingSelected(mode: String) {
        viewModelScope.launch {
            val changed = preferencesHelper.serverRoutingMode != mode
            _selectedServerRouting.emit(mode)
            preferencesHelper.serverRoutingMode = mode
            // Mark as manually configured
            if (!preferencesHelper.isAntiCensorshipManualMode) {
                preferencesHelper.isAntiCensorshipManualMode = true
            }
            if (changed){
                serverListRepository.update()
            }
        }
    }

    override fun refreshPreferences() {
        viewModelScope.launch {
            _protocolTweaksEnabled.emit(preferencesHelper.isProtocolTweaksEnabled)
            _selectedServerRouting.emit(preferencesHelper.serverRoutingMode)
        }
    }
}
