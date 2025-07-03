package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
data class AuthToken(
    @SerializedName("token") @Expose val token: String,
    @SerializedName("captcha") @Expose val captcha: Captcha? = null
)
@Keep
data class Captcha(
    @SerializedName("background") @Expose val background: String? = null,
    @SerializedName("slider") @Expose val slider: String? = null,
    @SerializedName("top") @Expose val top: Int? = null,
    @SerializedName("ascii_art") @Expose val asciiArt: String? = null,
)