/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class GetMyIpResponse {
    @SerializedName("user_ip")
    @Expose
    val userIp: String? = null

    override fun toString(): String =
        "UserIp{" +
            "user_ip=" + userIp +
            '}'
}
