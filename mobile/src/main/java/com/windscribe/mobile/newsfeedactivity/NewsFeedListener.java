/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.newsfeedactivity;

import com.windscribe.vpn.localdatabase.tables.WindNotification;

public interface NewsFeedListener {

    void onNotificationActionClick(WindNotification windNotification);

    void onNotificationExpand(WindNotification windNotification);
}
