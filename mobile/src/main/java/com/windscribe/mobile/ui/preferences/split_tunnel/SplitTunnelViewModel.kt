package com.windscribe.mobile.ui.preferences.split_tunnel

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.InstalledAppsData
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.SortByName
import com.windscribe.vpn.commonutils.SortBySelected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Collections

abstract class SplitTunnelViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val modes: List<DropDownStringItem>
    abstract val selectedModeKey: StateFlow<String>
    abstract val filteredApps: StateFlow<List<InstalledAppsData>>
    abstract val isSplitTunnelEnabled: StateFlow<Boolean>
    abstract val searchKeyword: StateFlow<String>
    open fun onModeSelected(mode: DropDownStringItem) {}
    open fun onAppSelected(app: InstalledAppsData) {}
    open fun onSplitTunnelSettingChanged() {}
    open fun onQueryTextChange(query: String) {}
}

class SplitTunnelViewModelImpl(val preferenceHelper: PreferencesHelper) : SplitTunnelViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    override val modes: List<DropDownStringItem>
        get() {
            val modes = appContext.resources.getStringArray(R.array.split_mode_list)
            val keys = appContext.resources.getStringArray(R.array.split_mode_list_keys)
            return keys.zip(modes).map { DropDownStringItem(it.first, it.second) }
        }
    private val _selectedModeKey = MutableStateFlow(preferenceHelper.splitRoutingMode)
    override val selectedModeKey: StateFlow<String> = _selectedModeKey
    private val _apps = MutableStateFlow(emptyList<InstalledAppsData>())
    private val _isSplitTunnelEnabled = MutableStateFlow(preferenceHelper.splitTunnelToggle)
    override val isSplitTunnelEnabled = _isSplitTunnelEnabled
    private val _searchKeyword = MutableStateFlow("")
    override val searchKeyword: StateFlow<String> = _searchKeyword
    private val _filteredApps = MutableStateFlow(emptyList<InstalledAppsData>())
    override val filteredApps: StateFlow<List<InstalledAppsData>> = _filteredApps


    init {
        loadApps(true)
    }

    private fun loadApps(initialLoad: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _showProgress.value = true
            val savedApps = preferenceHelper.installedApps()
            val pm = appContext.packageManager
            val installedApps =
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = mutableListOf<InstalledAppsData>()
            installedApps.forEach {
                val app = InstalledAppsData(
                    pm.getApplicationLabel(it).toString(),
                    it.packageName,
                    pm.getApplicationIcon(it)
                )
                for (installedAppsData in savedApps) {
                    if (app.packageName == installedAppsData) {
                        app.isChecked = true
                    }
                }
                appList.add(app)
            }
            Collections.sort(appList, SortByName())
            if (initialLoad) {
                Collections.sort(appList, SortBySelected())
            }
            _apps.emit(appList)
            _showProgress.value = false
            onQueryTextChange(_searchKeyword.value)
        }
    }

    override fun onModeSelected(mode: DropDownStringItem) {
        viewModelScope.launch {
            preferenceHelper.saveSplitRoutingMode(mode.key)
            _selectedModeKey.value = mode.key
        }
    }

    override fun onAppSelected(app: InstalledAppsData) {
        viewModelScope.launch(Dispatchers.IO) {
            _showProgress.value = true
            val apps = preferenceHelper.installedApps().toMutableList()
            if (app.isChecked) {
                apps.remove(app.packageName)
            } else {
                apps.add(app.packageName)
            }
            preferenceHelper.saveInstalledApps(apps.toList())
            loadApps()
        }
    }

    override fun onSplitTunnelSettingChanged() {
        viewModelScope.launch {
            val updatedState = _isSplitTunnelEnabled.value.not()
            _isSplitTunnelEnabled.emit(updatedState)
            preferenceHelper.splitTunnelToggle = updatedState
        }
    }

    override fun onQueryTextChange(query: String) {
        viewModelScope.launch {
            _searchKeyword.emit(query)
            if (query.isEmpty()) {
                _filteredApps.emit(_apps.value)
            } else {
                val apps = _apps.value.filter {
                    it.appName.contains(query, true)
                }
                _filteredApps.emit(apps)
            }
        }
    }
}