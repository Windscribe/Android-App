/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class BestLocationResponse {
    @SerializedName("city_name")
    @Expose
    val cityName: String? = null

    @SerializedName("country_code")
    @Expose
    val countryCode: String? = null

    @SerializedName("dc_id")
    @Expose
    val dcId: String? = null

    @SerializedName("hostname")
    @Expose
    val hostname: String? = null

    @SerializedName("ip")
    @Expose
    var ip: String? = null

    @SerializedName("ip2")
    @Expose
    var ip2: String? = null

    @SerializedName("ip3")
    @Expose
    var ip3: String? = null

    @SerializedName("location_name")
    @Expose
    val locationName: String? = null

    @SerializedName("server_id")
    @Expose
    val serverId: String? = null

    @SerializedName("short_name")
    @Expose
    val shortName: String? = null

    override fun toString(): String =
        "BestLocationResponse{" +
            "countryCode='" + countryCode + '\'' +
            ", shortName='" + shortName + '\'' +
            ", locationName='" + locationName + '\'' +
            ", cityName='" + cityName + '\'' +
            ", dcId='" + dcId + '\'' +
            ", serverId='" + serverId + '\'' +
            ", hostname='" + hostname + '\'' +
            ", ip='" + ip + '\'' +
            ", ip2='" + ip2 + '\'' +
            ", ip3='" + ip3 + '\'' +
            '}'
}
