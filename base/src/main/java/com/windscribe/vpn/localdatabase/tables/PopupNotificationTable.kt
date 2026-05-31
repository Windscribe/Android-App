/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_table")
class PopupNotificationTable(
    @PrimaryKey
    @ColumnInfo(name = "notification_id")
    val notificationId: Int,
    @ColumnInfo(name = "user_name")
    var userName: String?,
    @ColumnInfo(name = "popup_status")
    val popUpStatus: Int?,
)
