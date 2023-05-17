/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.StaticRegion
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxCompletable
import org.json.JSONObject

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
                val regions = localDbInterface.allStaticRegions.await()
                _events.emit(regions)
            }catch (e:Exception){}
        }
    }

    private suspend fun updateFromApi() {
        val sessionMap: MutableMap<String, String> = HashMap()
        preferencesHelper.getDeviceUUID(preferencesHelper.userName)?.let {
            sessionMap[NetworkKeyConstants.UUID_KEY] = it
        }
        val response = apiCallManager.getStaticIpList(sessionMap).await()
        val regions = response.dataClass?.let {
            val jsonObject = JSONObject(Gson().toJson(it))
            Gson().fromJson<List<StaticRegion>>(
                    jsonObject.getJSONArray("static_ips").toString(),
                    object : TypeToken<List<StaticRegion>?>() {}.type
            )
        } ?: emptyList()
        if (regions.isNotEmpty()) {
            regions[0].deviceName
        }
        localDbInterface.addStaticRegions(regions).await()
    }

    fun update(): Completable {
        return rxCompletable {
            updateFromApi()
        }
    }
}
