/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.constants

import com.windscribe.vpn.BuildConfig

object NetworkKeyConstants {
    const val API_HOST_GENERIC = "https://api."
    const val API_HOST_CHECK_IP = "https://checkip."
    const val API_HOST_ASSET = "https://assets."
    const val ALC_QUERY_KEY = "alc"
    const val BACKUP_HASH_CHAR_SET = "UTF-8"
    const val URL_FORGOT_PASSWORD = "/forgotpassword"
    const val URL_HELP_ME = "/support"
    const val URL_MY_ACCOUNT = "/myaccount?temp_session="
    const val URL_ADD_STATIC_IP = "/staticips"
    const val FIREBASE_DEVICE_ID_KEY = "gp_device_id"
    const val ADD_EMAIL_KEY = "email"
    const val REFERRING_USERNAME = "referring_username"
    const val ADD_EMAIL_FORCED_KEY = "email_forced"
    const val POST_LOG_FILE_KEY = "logfile"
    const val UUID_KEY = "device_id"
    const val STATIC_IP_TYPE_DATA_CENTER = "dc"
    const val NETWORK_REQUEST_CONNECTION_TIMEOUT: Long = 5
    const val NETWORK_REQUEST_READ_TIMEOUT: Long = 30
    const val NETWORK_REQUEST_WRITE_TIMEOUT: Long = 30
    const val SERVER_STATUS_TEMPORARILY_UNAVAILABLE = 2
    const val PING_TEST_3_BAR_UPPER_LIMIT = 150
    const val PING_TEST_2_BAR_UPPER_LIMIT = 500
    const val PING_TEST_1_BAR_UPPER_LIMIT = 1000
    const val SESSION_TYPE_ANDROID = "4"
    const val NODE_STATUS_URL = "/status"
    const val PORT_MAP_VERSION = 5
    const val URL_DISCORD = "https://discord.com/invite/vpn"
    const val URL_REDDIT = "https://www.reddit.com/r/Windscribe/"
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
    const val ROBERT = "/features/robert"
    var CLOUDFLARE_DOH = "https://cloudflare-dns.com"
    var GOOGLE_DOH = "https://dns.google"

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
