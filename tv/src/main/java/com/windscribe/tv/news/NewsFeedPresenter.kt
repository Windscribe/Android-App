/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.news

import com.windscribe.vpn.localdatabase.tables.NewsfeedAction

interface NewsFeedPresenter {
    fun init(showPopUp: Boolean, popUpId: Int)
    fun onActionClick(action: NewsfeedAction)
    fun onDestroy()
}
