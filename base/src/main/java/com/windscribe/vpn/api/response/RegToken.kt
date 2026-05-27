/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class RegToken {

    @SerializedName("id")
    @Expose
    val id: String? = null

    @SerializedName("signature")
    @Expose
    val signature: String? = null

    @SerializedName("time")
    @Expose
    val time: Long = 0

    @SerializedName("token")
    @Expose
    val token: String? = null
}
