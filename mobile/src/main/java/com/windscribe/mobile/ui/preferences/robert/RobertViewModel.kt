package com.windscribe.mobile.ui.preferences.robert

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.RobertFilter
import com.windscribe.vpn.api.response.RobertFilterResponse
import com.windscribe.vpn.api.response.WebSession
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.repository.CallResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class RobertViewModel : ViewModel() {
    abstract val robertFilterState: StateFlow<RobertFilterState>
    abstract val showProgress: StateFlow<Boolean>
    abstract val goToState: SharedFlow<RobertGoToState>
    open fun onManageRulesClick() {}
    open fun onFilterSettingChanged(robertFilter: RobertFilter, status: Int) {}
    open fun clearGoToState() {}
}

sealed class RobertGoToState {
    data class ManageRules(val url: String) : RobertGoToState()
    data class Toast(val message: ToastMessage) : RobertGoToState()
    object None : RobertGoToState()
}

sealed class RobertFilterState {
    object Loading : RobertFilterState()
    data class Success(val filters: List<RobertFilter>) : RobertFilterState()
    data class Failure(val error: String) : RobertFilterState()
}

class RobertViewModelImpl(
    val apiCallManager: IApiCallManager,
    val preferencesHelper: PreferencesHelper
) : RobertViewModel() {
    private val _robertFilterState = MutableStateFlow<RobertFilterState>(RobertFilterState.Loading)
    override val robertFilterState: StateFlow<RobertFilterState> = _robertFilterState
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    private val _goToState = MutableSharedFlow<RobertGoToState>(replay = 0)
    override val goToState: SharedFlow<RobertGoToState> = _goToState

    init {
        loadRobertFilters()
    }

    private fun loadRobertFilters() {
        viewModelScope.launch(Dispatchers.IO) {
            _showProgress.value = true
            val result = result<RobertFilterResponse> { apiCallManager.getRobertFilters() }
            _showProgress.value = false
            when (result) {
                is CallResult.Error -> {
                    val filters = loadFromDatabase()
                    if (filters.isNotEmpty()) {
                        _robertFilterState.value = RobertFilterState.Success(filters)
                    } else {
                        _robertFilterState.value =
                            RobertFilterState.Failure("Failed to load to Robert settings. Check your network connection.")
                    }
                }

                is CallResult.Success<RobertFilterResponse> -> {
                    saveToDatabase(result.data.filters)
                    _robertFilterState.value = RobertFilterState.Success(result.data.filters)
                }
            }
        }
    }

    private fun loadFromDatabase(): List<RobertFilter> {
        val json = preferencesHelper.robertFilters
        if (json == null) {
            return emptyList()
        }
        return Gson().fromJson(
            json,
            object : TypeToken<List<RobertFilter>>() {}.type
        )
    }

    private fun responseToUrl(webSession: WebSession): String {
        val uri = Uri.Builder()
            .scheme("https")
            .authority(NetworkKeyConstants.WEB_URL?.replace("https://", ""))
            .path("myaccount")
            .fragment("robertrules")
            .appendQueryParameter("temp_session", webSession.tempSession)
            .build()
        return uri.toString()
    }

    private fun saveToDatabase(filters: List<RobertFilter>) {
        val json = Gson().toJson(filters)
        preferencesHelper.robertFilters = json
    }

    override fun onManageRulesClick() {
        viewModelScope.launch {
            _showProgress.value = true
            val result = result<WebSession> { apiCallManager.getWebSession() }
            when (result) {
                is CallResult.Error -> {
                    _showProgress.value = false
                    if (result.code == NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA || result.code == NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API) {
                        _goToState.emit(RobertGoToState.Toast(ToastMessage.Localized(com.windscribe.vpn.R.string.no_internet)))
                    } else {
                        _goToState.emit(RobertGoToState.Toast(ToastMessage.Raw(result.errorMessage)))
                    }
                }

                is CallResult.Success -> {
                    _showProgress.value = false
                    _goToState.emit(RobertGoToState.ManageRules(responseToUrl(result.data)))
                }
            }
        }
    }

    override fun onFilterSettingChanged(robertFilter: RobertFilter, status: Int) {
        viewModelScope.launch {
            _showProgress.value = true
            val result = result<GenericSuccess> { apiCallManager.updateRobertSettings(robertFilter.id, status) }
            when (result) {
                is CallResult.Error -> {
                    _showProgress.value = false
                    _goToState.emit(RobertGoToState.Toast(ToastMessage.Localized(com.windscribe.vpn.R.string.failed_to_update_robert_rules)))
                    loadRobertFilters()
                }

                is CallResult.Success -> {
                    _showProgress.value = false
                    _goToState.emit(RobertGoToState.Toast(ToastMessage.Localized(com.windscribe.vpn.R.string.successfully_updated_robert_rules)))
                    appContext.workManager.updateRobertRules()
                    loadRobertFilters()
                }
            }
        }
    }

    override fun clearGoToState() {
        viewModelScope.launch {
            _goToState.emit(RobertGoToState.None)
        }
    }
}