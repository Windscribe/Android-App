package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class WgInitResponse(
    @SerializedName("config")
    @Expose
    val config: WgInitConfig
) : Serializable

@Keep
data class WgInitConfig(
    @SerializedName("PresharedKey")
    @Expose
    val preSharedKey: String,
    @SerializedName("AllowedIPs")
    @Expose
    val allowedIPs: String
)