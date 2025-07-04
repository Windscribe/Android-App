package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
data class SsoResponse(
    @SerializedName("session_auth_hash") @Expose val sessionAuth: String,
    @SerializedName("is_new_user") @Expose val isUser: Boolean
)