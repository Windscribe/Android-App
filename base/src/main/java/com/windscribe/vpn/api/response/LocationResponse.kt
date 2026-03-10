/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
data class LocationResponse(
    @SerializedName("locations")
    @Expose
    val locations: List<LocationData>
)

@Keep
data class LocationData(
    @SerializedName("id")
    @Expose
    val id: Int,

    @SerializedName("name")
    @Expose
    val name: String,

    @SerializedName("country_code")
    @Expose
    val countryCode: String,

    @SerializedName("short_name")
    @Expose
    val shortName: String,

    @SerializedName("premium_only")
    @Expose
    val premiumOnly: Int,

    @SerializedName("sort_order")
    @Expose
    val sortOrder: Int,

    @SerializedName("continent")
    @Expose
    val continent: String,

    @SerializedName("datacenters")
    @Expose
    val datacenters: List<DatacenterData>
)

@Keep
data class DatacenterData(
    @SerializedName("id")
    @Expose
    val id: Int,

    @SerializedName("city")
    @Expose
    val city: String,

    @SerializedName("nick")
    @Expose
    val nick: String?,

    @SerializedName("iata")
    @Expose
    val iata: String,

    @SerializedName("status")
    @Expose
    val status: Int,

    @SerializedName("gps")
    @Expose
    val gps: String,

    @SerializedName("tz")
    @Expose
    val tz: String,

    @SerializedName("p2p")
    @Expose
    val p2p: Int,

    @SerializedName("wg_pubkey")
    @Expose
    val wgPubkey: String,

    @SerializedName("wg_endpoint")
    @Expose
    val wgEndpoint: String,

    @SerializedName("ovpn_x509")
    @Expose
    val ovpnX509: String,

    @SerializedName("link_speed")
    @Expose
    val linkSpeed: Int,

    @SerializedName("premium")
    @Expose
    val premium: Int
)