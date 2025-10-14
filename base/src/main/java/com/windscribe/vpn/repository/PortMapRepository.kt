package com.windscribe.vpn.repository

import com.google.gson.Gson
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.exceptions.WindScribeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortMapRepository @Inject constructor(
    private val apiCallManager: IApiCallManager,
    private val preferencesHelper: PreferencesHelper
) {
    private var cachedPortMap: PortMapResponse? = null

    suspend fun getPortMap(): Result<PortMapResponse> {
        // Return cached if available
        cachedPortMap?.let { return Result.success(it) }

        val currentPortMap = preferencesHelper.portMapVersion
        if (currentPortMap != NetworkKeyConstants.PORT_MAP_VERSION) {
            return Result.failure(WindScribeException("Port map version outdated"))
        }

        // Try to load from preferences
        val cachedJson = preferencesHelper.getResponseString(PreferencesKeyConstants.PORT_MAP)
        val cachedResult = runCatching {
            Gson().fromJson(cachedJson, PortMapResponse::class.java)
        }.onSuccess { cachedPortMap = it }
            .mapCatching { Result.success(it) }
            .getOrNull()

        if (cachedResult != null) return cachedResult

        // Try API or fallback to hardcoded
        return if (WindUtilities.isOnline()) {
            getPortMapFromApi().recoverCatching {
                getHardCodedPortMap().getOrThrow()
            }
        } else {
            getHardCodedPortMap()
        }
    }

    fun getPortMapWithCallback(callback: (PortMapResponse) -> Unit) {
        cachedPortMap?.let {
            callback(it)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result = getPortMap()
            result.onSuccess { portMap ->
                cachedPortMap = portMap
                withContext(Dispatchers.Main) {
                    callback(portMap)
                }
            }.onFailure {
                // Silently fail - caller will handle missing callback
            }
        }
    }

    private fun getHardCodedPortMap(): Result<PortMapResponse> {
        return runCatching {
            appContext.resources.openRawResource(R.raw.port_map).use { inputStream ->
                val text = inputStream.bufferedReader().readText()
                Gson().fromJson(text, PortMapResponse::class.java)
            }
        }
    }

    private suspend fun getPortMapFromApi(): Result<PortMapResponse> {
        return when (val result = result<PortMapResponse> {
            apiCallManager.getPortMap()
        }) {
            is CallResult.Error -> {
                Result.failure(WindScribeException(result.errorMessage))
            }

            is CallResult.Success -> {
                preferencesHelper.savePortMapVersion(NetworkKeyConstants.PORT_MAP_VERSION)
                preferencesHelper.saveResponseStringData(
                    PreferencesKeyConstants.PORT_MAP,
                    Gson().toJson(result.data)
                )
                cachedPortMap = result.data
                Result.success(result.data)
            }
        }
    }

    fun clearCache() {
        cachedPortMap = null
    }
}
