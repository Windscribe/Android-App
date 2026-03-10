/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
@Entity(
    tableName = "Server",
    indices = [Index(value = ["server_id"], unique = true)]
)
data class Server(
    @SerializedName("id")
    @Expose
    @PrimaryKey
    @ColumnInfo(name = "server_id")
    val id: Int,

    @SerializedName("hostname")
    @Expose
    @ColumnInfo(name = "hostname")
    val hostname: String,

    @SerializedName("ip")
    @Expose
    @ColumnInfo(name = "ip")
    val ip: String,

    @SerializedName("ip2")
    @Expose
    @ColumnInfo(name = "ip2")
    val ip2: String,

    @SerializedName("ip3")
    @Expose
    @ColumnInfo(name = "ip3")
    val ip3: String,

    @SerializedName("datacenter_id")
    @Expose
    @ColumnInfo(name = "datacenter_id")
    val datacenterId: Int,

    @SerializedName("weight")
    @Expose
    @ColumnInfo(name = "weight")
    val weight: Int,

    @SerializedName("health")
    @Expose
    @ColumnInfo(name = "health")
    val health: Int,

    @SerializedName("ipv6")
    @Expose
    @ColumnInfo(name = "ipv6", defaultValue = "0")
    val ipv6: Int = 0
)