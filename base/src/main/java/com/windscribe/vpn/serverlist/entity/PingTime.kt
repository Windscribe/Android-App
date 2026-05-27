/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Keep
@Entity
class PingTime {

    @ColumnInfo(name = "static")
    var isStatic: Boolean = false

    @ColumnInfo(name = "ping_time")
    var pingTime: Int = 0

    @PrimaryKey
    @ColumnInfo(name = "ping_id")
    var ping_id: Int = 0

    @ColumnInfo(name = "isPro")
    var pro: Boolean = false

    @ColumnInfo(name = "region_id")
    var regionId: Int = 0

    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis()

    @ColumnInfo(name = "ip")
    var ip: String? = null

    @Ignore
    fun getId(): Int = ping_id

    @Ignore
    fun setId(id: Int) {
        this.ping_id = id
    }

    @Ignore
    fun isPro(): Boolean = pro

    @Ignore
    override fun toString(): String {
        return "PingTime{" +
                "ping_id=" + ping_id +
                ", pingTime=" + pingTime +
                ", regionId=" + regionId +
                ", isStatic=" + isStatic +
                ", pro=" + pro +
                '}'
    }
}
