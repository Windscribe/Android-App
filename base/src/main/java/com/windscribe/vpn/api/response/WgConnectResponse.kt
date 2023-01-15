package com.windscribe.vpn.api.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class WgConnectResponse(
        @SerializedName("config")
        @Expose
        val config: WgConnectConfig
) : Serializable

data class WgConnectConfig(
        @SerializedName("Address")
        @Expose
        val address: String,
        @SerializedName("DNS")
        @Expose
        val dns: String
)