/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class XPressLoginVerifyResponse {
    @SerializedName("session_auth_hash")
    @Expose
    val sessionAuth: String? = null

    override fun toString(): String =
        "XPressLoginVerifyResponse{" +
            "sessionAuth='" + sessionAuth + '\'' +
            '}'
}
