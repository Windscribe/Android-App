/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class ClaimAccountResponse {

    @SerializedName("password_updated")
    @Expose
    val passwordUpdated = 0

    @SerializedName("success")
    @Expose
    private val success = 0

    @SerializedName("username_updated")
    @Expose
    val usernameUpdated = 0

    val isSuccessful: Boolean
        get() = success == 1

    override fun toString(): String {
        return "ClaimAccountResponse{" +
                "passwordUpdated=" + passwordUpdated +
                ", usernameUpdated=" + usernameUpdated +
                ", success=" + success +
                '}'
    }
}
