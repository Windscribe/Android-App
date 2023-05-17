package com.windscribe.vpn.api

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.ApiConstants.AUTH_KEY_1
import com.windscribe.vpn.constants.ApiConstants.AUTH_KEY_2
import com.windscribe.vpn.constants.ApiConstants.AUTH_KEY_3
import com.windscribe.vpn.constants.ApiConstants.clientAuthHashKey
import com.windscribe.vpn.constants.ApiConstants.sessionAuthHashKey
import com.windscribe.vpn.constants.ApiConstants.sessionTimeKey
import com.windscribe.vpn.constants.ApiConstants.sessionTypeAndroid
import com.windscribe.vpn.constants.ApiConstants.sessionTypeKey

class AuthorizationGenerator(private val preferenceHelper: PreferencesHelper) {

    /**
     * Creates authentication map using session auth
     * to connect to the api
     */
    fun create(): Map<String, String> {
        val genericMap: MutableMap<String, String> = HashMap()
        preferenceHelper.sessionHash?.let {
            genericMap[sessionAuthHashKey] = it
        }
        val time = System.currentTimeMillis()
        genericMap[sessionTimeKey] = time.toString()
        val password = MD5Helper.md5(AUTH_KEY_1 + AUTH_KEY_2 + AUTH_KEY_3 + time)
        password.let { genericMap[clientAuthHashKey] = it }
        genericMap[sessionTypeKey] = sessionTypeAndroid
        return genericMap
    }
}
