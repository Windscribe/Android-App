/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class GenerateUsernameResponse {
    @SerializedName("username")
    @Expose
    val username: String? = null

    @SerializedName("success")
    @Expose
    val success: Int = 0

    val isSuccessful: Boolean
        get() = success == 1
}
