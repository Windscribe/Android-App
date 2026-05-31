/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_status_update")
class ServerStatusUpdateTable(
    @PrimaryKey
    @ColumnInfo(name = "user_name")
    var userName: String,
    @ColumnInfo(name = "server_status")
    val serverStatus: Int?,
)
