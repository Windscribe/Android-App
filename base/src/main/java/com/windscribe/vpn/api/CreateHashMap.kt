/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api

import android.os.Build
import com.windscribe.vpn.api.response.QueryType
import com.windscribe.vpn.constants.ApiConstants.CLAIM_ACCOUNT
import com.windscribe.vpn.constants.ApiConstants.SESSION_TYPE_KEY
import com.windscribe.vpn.constants.ApiConstants.SIGNATURE
import com.windscribe.vpn.constants.ApiConstants.TEMP_SESSION
import com.windscribe.vpn.constants.ApiConstants.TOKEN
import com.windscribe.vpn.constants.ApiConstants.TWO_FA_CODE
import com.windscribe.vpn.constants.ApiConstants.USER_NAME_KEY
import com.windscribe.vpn.constants.ApiConstants.USER_PASS_KEY
import com.windscribe.vpn.constants.ApiConstants.XPRESS_CODE

/**
 * Created by Mustafizur for Windscribe on 2017-09-15.
 */
object CreateHashMap {

    fun buildTicketMap(
            email: String,
            subject: String,
            message: String,
            username: String,
            queryType: QueryType
    ): Map<String, String?> {
        val queryMap: MutableMap<String, String> = HashMap()
        queryMap["support_email"] = email
        if (username.equals("na", ignoreCase = true)) {
            queryMap["support_name"] = "NULL"
        } else {
            queryMap["support_name"] = username
        }
        queryMap["support_subject"] = subject
        queryMap["support_message"] = message
        queryMap["support_category"] = queryType.value.toString()
        queryMap["issue_metadata[type]"] = queryType.name
        queryMap["issue_metadata[channel]"] = "app_android"
        val deviceInfo =
                String.format("%s | %s | %s", Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT)
        queryMap["issue_metadata[platform]"] = deviceInfo
        return queryMap
    }

    fun createClaimAccountMap(
            username: String,
            password: String
    ): MutableMap<String, String> {
        val paramMap: MutableMap<String, String> = HashMap()
        paramMap[USER_NAME_KEY] = username
        paramMap[USER_PASS_KEY] = password
        paramMap[CLAIM_ACCOUNT] = "1"
        return paramMap
    }

    fun createGhostModeMap(token: String): Map<String, String> {
        val paramMap: MutableMap<String, String> = HashMap()
        paramMap[TOKEN] = token
        return paramMap
    }

    fun createLoginMap(username: String, password: String, fa: String?): Map<String, String> {
        val paramMap: MutableMap<String, String> = HashMap()
        paramMap[USER_NAME_KEY] = username
        paramMap[USER_PASS_KEY] = password
        fa?.let {
            if (fa.isNotEmpty()) {
                paramMap[TWO_FA_CODE] = fa
            }
        }
        return paramMap
    }

    fun createRegistrationMap(username: String, password: String): Map<String, String> {
        val paramMap: MutableMap<String, String> = HashMap()
        paramMap[USER_NAME_KEY] = username
        paramMap[USER_PASS_KEY] = password
        return paramMap
    }

    fun createVerifyExpressLoginMap(code: String): Map<String, String> {
        val paramMap: MutableMap<String, String> = HashMap()
        paramMap[XPRESS_CODE] = code
        return paramMap
    }

    fun createVerifyXPressCodeMap(code: String, signature: String): Map<String, String> {
        val paramMap: MutableMap<String, String> = HashMap()
        paramMap[XPRESS_CODE] = code
        paramMap[SIGNATURE] = signature
        return paramMap
    }

    fun createWebSessionMap(): Map<String, String> {
        val paramMap: MutableMap<String, String> = HashMap()
        paramMap[SESSION_TYPE_KEY] = "1"
        paramMap[TEMP_SESSION] = "1"
        return paramMap
    }
}
