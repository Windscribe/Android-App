/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.StaticIPResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.StaticRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticIpRepository @Inject constructor(
    val scope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface,
) {
    private var _events = MutableStateFlow(emptyList<StaticRegion>())
    val regions: StateFlow<List<StaticRegion>> = _events

    init {
        load()
    }

    fun load() {
        scope.launch {
            try {
                val regions = localDbInterface.getAllStaticRegions()
                _events.emit(regions)
            } catch (e: Exception) {
            }
        }
    }

    suspend fun updateFromApi() {
        val result = result<StaticIPResponse> {
            apiCallManager.getStaticIpList(preferencesHelper.getDeviceUUID())
        }
        when (result) {
            is CallResult.Error -> {}
            is CallResult.Success -> {
                val regions = result.data.let {
                    val jsonObject = JSONObject(Gson().toJson(it))
                    Gson().fromJson<List<StaticRegion>>(
                        jsonObject.getJSONArray("static_ips").toString(),
                        object : TypeToken<List<StaticRegion>?>() {}.type
                    )
                } ?: emptyList()
                if (regions.isNotEmpty()) {
                    regions[0].deviceName
                }
                localDbInterface.addStaticRegions(regions)
            }
        }
    }
}
