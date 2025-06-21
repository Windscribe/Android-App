/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
data class RobertSettingsResponse(
    @SerializedName("lists") @Expose
    var list: List<String>? = null,

    @SerializedName("settings") @Expose
    var settings: List<String>? = null
)
