/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.newsfeedactivity

import com.windscribe.vpn.localdatabase.tables.WindNotification

interface NewsFeedListener {
    fun onNotificationActionClick(windNotification: WindNotification)
    fun onNotificationExpand(windNotification: WindNotification)
}