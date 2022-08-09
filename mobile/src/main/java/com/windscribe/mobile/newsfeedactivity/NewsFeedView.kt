/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.newsfeedactivity

import com.windscribe.mobile.adapter.NewsFeedAdapter
import com.windscribe.vpn.api.response.PushNotificationAction

interface NewsFeedView {
    fun hideProgress()
    fun setNewsFeedAdapter(mAdapter: NewsFeedAdapter)
    fun showLoadingError(errorMessage: String)
    fun showProgress(progressTitle: String)
    fun startUpgradeActivity(pushNotificationAction: PushNotificationAction)
}