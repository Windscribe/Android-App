/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.localdatabase.tables.WindNotification

data class NewsFeedNotification(
        @SerializedName("notifications")
        @Expose
        val notifications: List<WindNotification>?
)
