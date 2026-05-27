/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class ApiErrorResponse {

    @SerializedName("errorCode")
    @Expose
    var errorCode: Int? = null

    @SerializedName("errorDescription")
    @Expose
    val errorDescription: String? = null

    @SerializedName("errorMessage")
    @Expose
    var errorMessage: String? = null

    @SerializedName("logStatus")
    @Expose
    val logStatus: String? = null

    override fun toString(): String {
        return "ErrorResponse{" +
                "errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorDescription='" + errorDescription + '\'' +
                ", logStatus='" + logStatus + '\'' +
                '}'
    }
}
