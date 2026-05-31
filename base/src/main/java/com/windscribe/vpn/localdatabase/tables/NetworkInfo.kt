/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Network_Info")
class NetworkInfo(
    @PrimaryKey
    var networkName: String,
    @ColumnInfo(name = "is_auto_secure")
    var isAutoSecureOn: Boolean,
    @ColumnInfo(name = "is_preferred")
    var isPreferredOn: Boolean,
    @ColumnInfo(name = "protocol")
    var protocol: String?,
    @ColumnInfo(name = "port")
    var port: String?,
) {
    override fun toString(): String =
        "NetworkInfo{" +
            "networkName='" + networkName + '\'' +
            ", isAutoSecureOn=" + isAutoSecureOn +
            ", isPreferredOn=" + isPreferredOn +
            ", protocol='" + protocol + '\'' +
            ", port='" + port + '\'' +
            '}'
}
