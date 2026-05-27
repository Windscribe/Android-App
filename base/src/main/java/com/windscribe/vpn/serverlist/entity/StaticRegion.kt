/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.windscribe.vpn.api.response.ServerCredentialsResponse

@Keep
@Entity(tableName = "StaticRegion")
class StaticRegion {

    @SerializedName("city_name")
    @Expose
    var cityName: String? = null

    @SerializedName("country_code")
    @Expose
    var countryCode: String? = null

    @SerializedName("credentials")
    @Expose
    @Embedded
    var credentials: ServerCredentialsResponse? = null

    @SerializedName("device_name")
    @Expose
    var deviceName: String? = null

    @PrimaryKey
    @SerializedName("id")
    @Expose
    var id: Int? = null

    @SerializedName("ip_id")
    @Expose
    var ipId: Int? = null

    @SerializedName("name")
    @Expose
    var name: String? = null

    @SerializedName("node")
    @Expose
    @Embedded(prefix = "node")
    var nodeStatic: NodeStatic? = null

    @SerializedName("ovpn_x509")
    @Expose
    var ovpnX509: String? = null

    @SerializedName("server_id")
    @Expose
    var serverId: Int? = null

    @SerializedName("short_name")
    @Expose
    var shortName: String? = null

    @SerializedName("static_ip")
    @Expose
    var staticIp: String? = null

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("wg_ip")
    @Expose
    var wgIp: String? = null

    @SerializedName("wg_pubkey")
    @Expose
    var wgPubKey: String? = null

    @SerializedName("ping_host")
    @Expose
    var pingHost: String? = null

    @SerializedName("status")
    @Expose
    var status: Int? = null

    @SerializedName("gps")
    @Expose
    @ColumnInfo(name = "gps")
    var coordinates: String? = null

    fun getStaticIpNode(): NodeStatic? = nodeStatic

    override fun toString(): String {
        return "StaticRegion{" +
                "id=" + id +
                ", ipId=" + ipId +
                ", staticIp='" + staticIp + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", shortName='" + shortName + '\'' +
                ", cityName='" + cityName + '\'' +
                ", serverId=" + serverId +
                ", nodeStatic=" + nodeStatic +
                ", credentials=" + credentials +
                ", deviceName='" + deviceName + '\'' +
                ", OvpnX509='" + ovpnX509 + '\'' +
                ", pingHost='" + pingHost + '\'' +
                ", status='" + status + '\'' +
                '}'
    }
}
