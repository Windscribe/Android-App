package com.windscribe.vpn.api.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AuthToken(
    @SerializedName("token") @Expose val token: String,
    @SerializedName("captcha") @Expose val captcha: Captcha? = null

)

data class Captcha(
    @SerializedName("background") @Expose val background: String,
    @SerializedName("slider") @Expose val slider: String,
    @SerializedName("top") @Expose val top: Int,
)