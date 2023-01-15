/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.model

import com.google.gson.Gson
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.serverlist.entity.StaticRegion

class StaticConfig(staticRegion: StaticRegion) {

    val cityName: String
    lateinit var coordinatesArray: Array<String>
    val countryCode: String
    val id: Int
    val ip1: String
    val ip2: String
    val ip3: String
    val staticIp: String
    val wgIp: String
    val wgPubKey: String
    val x509Name: String

    init {
        val node = staticRegion.staticIpNode
        ip1 = node.hostname
        ip2 = node.ip2
        ip3 = node.ip3
        countryCode = staticRegion.countryCode
        staticIp = staticRegion.staticIp
        cityName = staticRegion.cityName
        id = staticRegion.id
        wgIp = staticRegion.wgIp
        wgPubKey = staticRegion.wgPubKey
        x509Name = staticRegion.ovpnX509
        appContext.preference.saveCredentials(PreferencesKeyConstants.STATIC_IP_CREDENTIAL,staticRegion.credentials)
    }
}
