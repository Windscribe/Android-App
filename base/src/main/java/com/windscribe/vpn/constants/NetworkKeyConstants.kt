/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.constants

import com.windscribe.vpn.BuildConfig

object NetworkKeyConstants {
    const val URL_FORGOT_PASSWORD = "/forgotpassword"
    const val URL_HELP_ME = "/support"
    const val URL_MY_ACCOUNT = "/myaccount?temp_session="
    const val URL_ADD_STATIC_IP = "/staticips"
    const val NETWORK_REQUEST_CONNECTION_TIMEOUT: Long = 5
    const val SERVER_STATUS_TEMPORARILY_UNAVAILABLE = 2
    const val PING_TEST_3_BAR_UPPER_LIMIT = 150
    const val PING_TEST_2_BAR_UPPER_LIMIT = 500
    const val PING_TEST_1_BAR_UPPER_LIMIT = 1000
    const val SESSION_TYPE_ANDROID = "4"
    const val NODE_STATUS_URL = "/status"
    const val PORT_MAP_VERSION = 5
    const val URL_DISCORD = "https://discord.com/invite/vpn"
    const val URL_REDDIT = "https://www.reddit.com/r/Windscribe/"
    const val URL_YOUTUBE = "https://www.youtube.com/c/Windscribe"
    const val URL_X = "https://x.com/windscribecom"
    const val URL_GARRY = "/support?garry=1"
    const val URL_KNOWLEDGE = "/support/knowledgebase"
    const val URL_STATUS = "/status"
    const val URL_ABOUT = "/about"
    const val URL_TERMS = "/terms"
    const val URL_VIEW_LICENCE = "/terms/oss"
    const val URL_CHANGELOG = "/changelog/android"
    const val URL_PRIVACY = "/privacy"
    const val URL_BLOG = "https://blog.windscribe.com/"
    const val URL_JOB = "/jobs"

    @JvmField
    var WEB_URL: String? = null

    @JvmStatic
    fun getWebsiteLink(url: String): String {
        return WEB_URL + url
    }

    init {
        if (BuildConfig.DEV) {
            // Staging Environment
            WEB_URL = BuildConfig.STAGING_WEB_URL
        } else {
            // Production Environment
            WEB_URL = BuildConfig.PRODUCTION_WEB_URL
        }
    }
}
