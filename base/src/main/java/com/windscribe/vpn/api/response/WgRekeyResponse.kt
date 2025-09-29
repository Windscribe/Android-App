package com.windscribe.vpn.api.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class WgRekeyResponse(
    @SerializedName("config")
    @Expose
    val config: RekeyConfig,
) : Serializable

data class RekeyConfig(
    @SerializedName("PublicKey")
    @Expose
    val publicKey: String,
    @SerializedName("PresharedKey")
    @Expose
    val presharedKey: String,
)