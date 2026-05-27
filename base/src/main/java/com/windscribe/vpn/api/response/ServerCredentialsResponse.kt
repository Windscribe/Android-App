/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Created by Mustafizur on 2017-11-30.
 */
@Keep
class ServerCredentialsResponse {

    @SerializedName("password")
    @Expose
    var passwordEncoded: String? = null

    @SerializedName("username")
    @Expose
    var userNameEncoded: String? = null

    override fun toString(): String {
        return "ServerCredentialsResponse{" +
                "userNameEncoded='" + mask(userNameEncoded) + '\'' +
                ", passwordEncoded='" + mask(passwordEncoded) + '\'' +
                '}'
    }

    private companion object {
        fun mask(value: String?): String {
            if (value.isNullOrEmpty()) return "****"
            if (value.length <= 4) return "****"
            val maskedLength = value.length - 4
            return "*".repeat(maskedLength) + value.substring(value.length - 4)
        }
    }
}
