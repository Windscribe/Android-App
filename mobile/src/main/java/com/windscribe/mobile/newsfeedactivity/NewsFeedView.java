/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.newsfeedactivity;

import com.windscribe.mobile.adapter.NewsFeedAdapter;
import com.windscribe.vpn.api.response.PushNotificationAction;


public interface NewsFeedView {

    void hideProgress();

    void setNewsFeedAdapter(NewsFeedAdapter mAdapter);

    void showLoadingError(String errorMessage);

    void showProgress(String progressTitle);

    void startUpgradeActivity(PushNotificationAction pushNotificationAction);
}
