/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.localdatabase.tables.WindNotification

@Keep
data class NewsFeedNotification(
        @SerializedName("notifications")
        @Expose
        val notifications: List<WindNotification>?
)
