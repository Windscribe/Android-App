/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@SuppressWarnings("unused")
@Entity(tableName = "server_status_update")
public class ServerStatusUpdateTable {

    @ColumnInfo(name = "server_status")
    private final Integer serverStatus;

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_name")
    private String userName;


    public ServerStatusUpdateTable(@NonNull String userName, Integer serverStatus) {
        this.userName = userName;
        this.serverStatus = serverStatus;
    }

    public Integer getServerStatus() {
        return serverStatus;
    }

    @NonNull
    public String getUserName() {
        return userName;
    }

    public void setUserName(@NonNull String userName) {
        this.userName = userName;
    }
}
