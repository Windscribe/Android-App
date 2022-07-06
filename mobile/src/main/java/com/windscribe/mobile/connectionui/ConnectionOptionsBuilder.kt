/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionui

import com.windscribe.vpn.localdatabase.tables.NetworkInfo

class ConnectionOptionsBuilder {
    private var networkInfo: NetworkInfo? = null
    fun build(): ConnectionOptions {
        return ConnectionOptions(networkInfo)
    }

    fun setNetworkInfo(networkInfo: NetworkInfo?): ConnectionOptionsBuilder {
        this.networkInfo = networkInfo
        return this
    }
}