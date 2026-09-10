/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A row of `Network_Info`, published through a `StateFlow` by `NetworkInfoManager`.
 *
 * `data` and `val` are both required: the flow conflates equal values, and Room returns a fresh
 * object per read. Without value equality every reload emits again; without immutability, mutating
 * the published value makes the next read compare equal and emit nothing.
 *
 * Change a setting with [copy] and publish the result.
 */
@Entity(tableName = "Network_Info")
data class NetworkInfo(
    @PrimaryKey
    val networkName: String,
    @ColumnInfo(name = "is_auto_secure")
    val isAutoSecureOn: Boolean,
    @ColumnInfo(name = "is_preferred")
    val isPreferredOn: Boolean,
    @ColumnInfo(name = "protocol")
    val protocol: String?,
    @ColumnInfo(name = "port")
    val port: String?,
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
