package com.windscribe.mobile.ui.helper

import com.google.gson.Gson
import com.windscribe.vpn.R.raw
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.CallResult

object PortMapLoader {

    internal suspend fun getPortMap(
        api: IApiCallManager,
        preferencesHelper: PreferencesHelper
    ): Result<PortMapResponse> {
        val currentPortMap = preferencesHelper.portMapVersion
        if (currentPortMap != NetworkKeyConstants.PORT_MAP_VERSION) {
            return Result.failure(WindScribeException("Port map version outdated"))
        }

        val cachedJson = preferencesHelper.getResponseString(PreferencesKeyConstants.PORT_MAP)
        val cachedResult = runCatching {
            Gson().fromJson(cachedJson, PortMapResponse::class.java)
        }.mapCatching { Result.success(it) }
            .getOrNull()

        if (cachedResult != null) return cachedResult

        return if (WindUtilities.isOnline()) {
            getPortMapFromApi(api, preferencesHelper).recoverCatching {
                getHardCodedPortMap().getOrThrow()
            }
        } else {
            getHardCodedPortMap()
        }
    }

    private fun getHardCodedPortMap(): Result<PortMapResponse> {
        return runCatching {
            appContext.resources.openRawResource(raw.port_map).use { inputStream ->
                val text = inputStream.bufferedReader().readText()
                Gson().fromJson(text, PortMapResponse::class.java)
            }
        }
    }

    private suspend fun getPortMapFromApi(
        api: IApiCallManager,
        preferencesHelper: PreferencesHelper
    ): Result<PortMapResponse> {
        return when (val result = api.getPortMap().result<PortMapResponse>()) {
            is CallResult.Error -> {
                Result.failure(WindScribeException(result.errorMessage))
            }

            is CallResult.Success -> {
                preferencesHelper.savePortMapVersion(NetworkKeyConstants.PORT_MAP_VERSION)
                preferencesHelper.saveResponseStringData(
                    PreferencesKeyConstants.PORT_MAP,
                    Gson().toJson(result.data)
                )
                Result.success(result.data)
            }
        }
    }
}