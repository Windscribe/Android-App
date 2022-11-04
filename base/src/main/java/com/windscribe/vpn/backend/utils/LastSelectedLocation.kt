/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import java.io.Serializable

class LastSelectedLocation(
        val cityId: Int,
        var nodeName: String = "Custom Config",
        val nickName: String,
        val countryCode: String? = "NA",
        val lat: String? = "52.23",
        val lang: String? = "32.22"
) : Serializable {
    override fun toString(): String {
        return "cityId=$cityId nodeName=$nodeName nickName=$nickName countryCode=$countryCode lat=$lat lang=$lang)"
    }
}
