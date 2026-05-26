package com.windscribe.mobile.ui.preferences.anticensorship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.repository.UnblockWgParamsRepository
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class AntiCensorshipViewModel : ViewModel() {
    abstract val protocolTweaksModes: StateFlow<List<DropDownStringItem>>
    abstract val selectedProtocolTweaksMode: StateFlow<String>
    abstract val amneziaPresets: StateFlow<List<DropDownStringItem>>
    abstract val selectedPreset: StateFlow<String>
    abstract val extraTlsPaddingEnabled: StateFlow<Boolean>
    abstract val serverRoutingModes: StateFlow<List<DropDownStringItem>>
    abstract val selectedServerRouting: StateFlow<String>
    abstract fun onProtocolTweaksModeSelected(mode: String)
    abstract fun onAmneziaPresetSelected(presetId: String)
    abstract fun onExtraTlsPaddingToggled()
    abstract fun onServerRoutingSelected(mode: String)
    abstract fun refreshPreferences()
}

@HiltViewModel
class AntiCensorshipViewModelImpl @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val unblockWgParamsRepository: UnblockWgParamsRepository,
    private val workManager: WindScribeWorkManager
) : AntiCensorshipViewModel() {

    private val _protocolTweaksModes = MutableStateFlow(
        listOf(
            DropDownStringItem(PreferencesKeyConstants.PROTOCOL_TWEAKS_AUTO, "Auto"),
            DropDownStringItem(PreferencesKeyConstants.PROTOCOL_TWEAKS_MANUAL, "Enabled"),
            DropDownStringItem(PreferencesKeyConstants.PROTOCOL_TWEAKS_DISABLED, "Disabled")
        )
    )
    override val protocolTweaksModes: StateFlow<List<DropDownStringItem>> = _protocolTweaksModes

    private val _selectedProtocolTweaksMode = MutableStateFlow(preferencesHelper.protocolTweaksMode)
    override val selectedProtocolTweaksMode: StateFlow<String> = _selectedProtocolTweaksMode

    private val _amneziaPresets = MutableStateFlow(emptyList<DropDownStringItem>())
    override val amneziaPresets: StateFlow<List<DropDownStringItem>> = _amneziaPresets

    private val _selectedPreset = MutableStateFlow(
        unblockWgParamsRepository.getSelectedUnblockWgParam()?.id ?: ""
    )
    override val selectedPreset: StateFlow<String> = _selectedPreset

    private val _extraTlsPaddingEnabled = MutableStateFlow(preferencesHelper.extraTlsPaddingEnabled)
    override val extraTlsPaddingEnabled: StateFlow<Boolean> = _extraTlsPaddingEnabled

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

    override fun onProtocolTweaksModeSelected(mode: String) {
        viewModelScope.launch {
            _selectedProtocolTweaksMode.emit(mode)
            preferencesHelper.protocolTweaksMode = mode

            // Mark as manually configured only if not Auto
            if (mode != PreferencesKeyConstants.PROTOCOL_TWEAKS_AUTO) {
                if (!preferencesHelper.isAntiCensorshipManualMode) {
                    preferencesHelper.isAntiCensorshipManualMode = true
                }
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

    override fun onExtraTlsPaddingToggled() {
        viewModelScope.launch {
            val newValue = !_extraTlsPaddingEnabled.value
            _extraTlsPaddingEnabled.emit(newValue)
            preferencesHelper.extraTlsPaddingEnabled = newValue
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
                workManager.updateServerList()
            }
        }
    }

    override fun refreshPreferences() {
        viewModelScope.launch {
            _selectedProtocolTweaksMode.emit(preferencesHelper.protocolTweaksMode)
            _extraTlsPaddingEnabled.emit(preferencesHelper.extraTlsPaddingEnabled)
            _selectedServerRouting.emit(preferencesHelper.serverRoutingMode)
        }
    }
}
