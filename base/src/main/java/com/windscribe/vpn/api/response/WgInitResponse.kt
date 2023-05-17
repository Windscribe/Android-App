package com.windscribe.vpn.api.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class WgInitResponse(
        @SerializedName("config")
        @Expose
        val config: WgInitConfig
) : Serializable

data class WgInitConfig(
        @SerializedName("PresharedKey")
        @Expose
        val preSharedKey: String,
        @SerializedName("AllowedIPs")
        @Expose
        val allowedIPs: String
)