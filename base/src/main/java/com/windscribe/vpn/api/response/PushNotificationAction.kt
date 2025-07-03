/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class PushNotificationAction(
        @SerializedName("pcpid")
        @Expose
        val pcpID: String,

        @SerializedName("promo_code")
        @Expose
        val promoCode: String,

        @SerializedName("type")
        @Expose
        val type: String
) : Serializable
