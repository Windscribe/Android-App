package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class WgInitResponse(
    @SerializedName("config")
    val config: WgInitConfig,
) : Serializable

@Keep
data class WgInitConfig(
    @SerializedName("PresharedKey")
    val preSharedKey: String,
    @SerializedName("AllowedIPs")
    val allowedIPs: String,
    @SerializedName("AllowedIPsV6")
    val allowedIPsV6: String,
    @SerializedName("HashedCIDR")
    val hashedCIDR: List<String>,
    @SerializedName("HashedCIDRv6")
    val hashedCIDRV6: List<String>,
) : Serializable
