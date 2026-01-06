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
    abstract val showSystemApps: StateFlow<Boolean>
    open fun onModeSelected(mode: DropDownStringItem) {}
    open fun onAppSelected(app: InstalledAppsData) {}
    open fun onSplitTunnelSettingChanged() {}
    open fun onQueryTextChange(query: String) {}
    open fun onShowSystemAppsToggle() {}
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
    private val _showSystemApps = MutableStateFlow(preferenceHelper.showSystemApps)
    override val showSystemApps: StateFlow<Boolean> = _showSystemApps
    // Store current app order positions to maintain consistency
    private var currentAppOrder = mutableMapOf<String, Int>()


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
                val isSystemApp = (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 1
                val app = InstalledAppsData(
                    pm.getApplicationLabel(it).toString(),
                    it.packageName,
                    pm.getApplicationIcon(it)
                )
                app.isSystemApp = isSystemApp
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
                // Store the initial order after sorting by selected status
                currentAppOrder.clear()
                appList.forEachIndexed { index, app ->
                    currentAppOrder[app.packageName] = index
                }
            }
            _apps.emit(appList)
            _showProgress.value = false
            applyFilters()
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
            updateSavedApps(app)
            updateAppListInPlace(app)
        }
    }

    private fun updateSavedApps(app: InstalledAppsData) {
        val apps = preferenceHelper.installedApps().toMutableList()
        if (app.isChecked) {
            apps.remove(app.packageName)
        } else {
            apps.add(app.packageName)
        }
        preferenceHelper.saveInstalledApps(apps.toList())
    }

    private suspend fun updateAppListInPlace(app: InstalledAppsData) {
        val currentApps = _apps.value.toMutableList()
        val appIndex = currentApps.indexOfFirst { it.packageName == app.packageName }
        if (appIndex != -1) {
            currentApps[appIndex].isChecked = !currentApps[appIndex].isChecked
            val newAppsList = createNewAppsList(currentApps)
            newAppsList[appIndex].isChecked = currentApps[appIndex].isChecked
            emitUpdatedLists(newAppsList)
        }
    }

    private fun createNewAppsList(currentApps: MutableList<InstalledAppsData>): MutableList<InstalledAppsData> {
        return currentApps.map { appData ->
            val newApp = InstalledAppsData(appData.appName, appData.packageName, appData.appIconDrawable)
            newApp.isChecked = appData.isChecked
            newApp
        }.toMutableList()
    }

    private suspend fun emitUpdatedLists(newAppsList: MutableList<InstalledAppsData>) {
        _apps.emit(newAppsList)
        val query = _searchKeyword.value
        if (query.isEmpty()) {
            _filteredApps.emit(newAppsList)
        } else {
            val filteredApps = newAppsList.filter {
                it.appName.contains(query, true)
            }
            _filteredApps.emit(filteredApps)
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
            applyFilters()
        }
    }

    override fun onShowSystemAppsToggle() {
        viewModelScope.launch {
            val updatedState = _showSystemApps.value.not()
            _showSystemApps.emit(updatedState)
            preferenceHelper.showSystemApps = updatedState
            applyFilters()
        }
    }

    private suspend fun applyFilters() {
        val query = _searchKeyword.value
        val showSystem = _showSystemApps.value
        val allApps = _apps.value

        val filtered = allApps.filter { app ->
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                app.appName.contains(query, true)
            }
            val matchesSystemFilter = if (showSystem) {
                true
            } else {
                !app.isSystemApp
            }
            matchesSearch && matchesSystemFilter
        }
        _filteredApps.emit(filtered)
    }
}