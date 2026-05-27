/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ping_results")
class PingTestResults(
    @PrimaryKey
    @ColumnInfo(name = "node_name")
    var nodeName: String,
    @ColumnInfo(name = "node_parent_index")
    val nodeParentIndex: Int,
    @ColumnInfo(name = "node_ping_time")
    val nodePingTime: Int?
)
