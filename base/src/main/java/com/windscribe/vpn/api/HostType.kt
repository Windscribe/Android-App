package com.windscribe.vpn.api

import com.windscribe.vpn.constants.NetworkKeyConstants

enum class HostType {
    API, ASSET, CHECK_IP;

    val text: String
        get() {
            return when (this) {
                API -> return NetworkKeyConstants.API_HOST_GENERIC
                ASSET -> NetworkKeyConstants.API_HOST_ASSET
                CHECK_IP -> NetworkKeyConstants.API_HOST_CHECK_IP
            }
        }
}
