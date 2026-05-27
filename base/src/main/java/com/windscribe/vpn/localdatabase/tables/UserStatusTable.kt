/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_account_info")
class UserStatusTable(
    @PrimaryKey
    @ColumnInfo(name = "user_name")
    var userName: String,
    @ColumnInfo(name = "is_premium")
    val isPremium: Int?,
    @ColumnInfo(name = "account_status")
    val accountStatus: Int?
) {
    override fun toString(): String {
        return "UserStatusTable{" +
                ", isPremium=" + isPremium +
                ", accountStatus=" + accountStatus +
                '}'
    }
}
