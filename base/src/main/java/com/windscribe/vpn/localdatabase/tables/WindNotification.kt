/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "WindNotification")
data class WindNotification(
    @ColumnInfo(name = "id")
    @SerializedName("id")
    @Expose
    @PrimaryKey
    val notificationId: Int,
    @ColumnInfo(name = "date")
    @SerializedName("date")
    @Expose
    var notificationDate: Long,
    @ColumnInfo(name = "message")
    @SerializedName("message")
    @Expose
    var notificationMessage: String,
    @ColumnInfo(name = "perm_free")
    @SerializedName("perm_free")
    @Expose
    var notificationPermFree: Long,
    @ColumnInfo(name = "perm_pro")
    @SerializedName("perm_pro")
    @Expose
    var notificationPermPro: Long,
    @ColumnInfo(name = "title")
    @SerializedName("title")
    @Expose
    var notificationTitle: String,
    @ColumnInfo(name = "popup")
    @SerializedName("popup")
    @Expose
    var popUp: Int,
    @SerializedName("action")
    @Expose
    @Embedded
    var action: NewsfeedAction?,
    var isRead: Boolean = false

)
