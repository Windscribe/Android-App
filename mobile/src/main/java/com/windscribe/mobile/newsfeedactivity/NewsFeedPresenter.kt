/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.newsfeedactivity

interface NewsFeedPresenter {
    fun init(showPopUp: Boolean, popUpId: Int)
    fun onDestroy()
}