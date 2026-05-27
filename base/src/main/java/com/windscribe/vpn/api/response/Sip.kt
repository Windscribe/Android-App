/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Suppress("unused")
@Keep
class Sip {

    @SerializedName("update")
    @Expose
    val update: List<String>? = null

    @SerializedName("count")
    @Expose
    val count: Int? = null
}
