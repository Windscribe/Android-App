/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class NodeStatic(
    @SerializedName("city_name")
    @Expose
    var cityName: String? = null,
    @SerializedName("dns_hostname")
    @Expose
    var dnsHostname: String? = null,
    @SerializedName("hostname")
    @Expose
    var hostname: String? = null,
    @SerializedName("ip")
    @Expose
    var ip: String? = null,
    @SerializedName("ip2")
    @Expose
    var ip2: String? = null,
    @SerializedName("ip3")
    @Expose
    var ip3: String? = null,
)
