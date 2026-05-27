/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class XPressLoginCodeResponse {
    @SerializedName("sig")
    @Expose
    var signature: String? = null

    @SerializedName("time")
    @Expose
    var time: Long = 0

    @SerializedName("ttl")
    @Expose
    var ttl: Int = 0

    @SerializedName("xpress_code")
    @Expose
    var xPressLoginCode: String? = null

    override fun toString(): String =
        "XPressLoginCodeResponse{" +
            "xPressLoginCode='" + xPressLoginCode + '\'' +
            ", ttl=" + ttl +
            ", signature='" + signature + '\'' +
            ", time=" + time +
            '}'
}
