/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.serverlist.entity.StaticRegion

@Keep
class StaticIPResponse {
    @SerializedName("static_ips")
    @Expose
    var staticIpList: List<StaticRegion>? = null
}
