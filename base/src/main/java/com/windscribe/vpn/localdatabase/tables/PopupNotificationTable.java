/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@SuppressWarnings("unused")
@Entity(tableName = "notification_table")
public class PopupNotificationTable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "notification_id")
    private final Integer notificationId;

    @ColumnInfo(name = "popup_status")
    private final Integer popUpStatus;

    @ColumnInfo(name = "user_name")
    private String userName;


    public PopupNotificationTable(@NonNull Integer notificationId, String userName, Integer popUpStatus) {
        this.notificationId = notificationId;
        this.userName = userName;
        this.popUpStatus = popUpStatus;
    }

    @NonNull
    public Integer getNotificationId() {
        return notificationId;
    }

    public Integer getPopUpStatus() {
        return popUpStatus;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
