/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.news

import com.windscribe.tv.adapter.NewsFeedAdapter
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.localdatabase.tables.NewsfeedAction

interface NewsFeedView {
    fun setActionLabel(action: NewsfeedAction)
    fun setItemSelected(notificationId: Int)
    fun setNewsFeedAdapter(mAdapter: NewsFeedAdapter)
    fun setNewsFeedContentText(contentText: String)
    fun showLoadingError(errorMessage: String)
    fun startUpgradeActivity(pushNotificationAction: PushNotificationAction)
}
