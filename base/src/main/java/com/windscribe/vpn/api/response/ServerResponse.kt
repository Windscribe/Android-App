/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
data class ServerResponse(
    @SerializedName("servers")
    @Expose
    val servers: List<ServerData>,

    @SerializedName("revision")
    @Expose
    val revision: Long
)

@Keep
data class ServerData(
    @SerializedName("id")
    @Expose
    val id: Int,

    @SerializedName("hostname")
    @Expose
    val hostname: String,

    @SerializedName("ip")
    @Expose
    val ip: String,

    @SerializedName("ip2")
    @Expose
    val ip2: String,

    @SerializedName("ip3")
    @Expose
    val ip3: String,

    @SerializedName("datacenter_id")
    @Expose
    val datacenterId: Int,

    @SerializedName("weight")
    @Expose
    val weight: Int,

    @SerializedName("health")
    @Expose
    val health: Int
)