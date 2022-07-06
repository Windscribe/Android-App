/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@SuppressWarnings("unused")
@Entity(tableName = "user_account_info")
public class UserStatusTable {

    @ColumnInfo(name = "account_status")
    private final Integer accountStatus;

    @ColumnInfo(name = "is_premium")
    private final Integer isPremium;

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_name")
    private String userName;


    public UserStatusTable(@NonNull String userName, Integer isPremium,
            Integer accountStatus) {
        this.userName = userName;
        this.isPremium = isPremium;
        this.accountStatus = accountStatus;
    }

    public Integer getAccountStatus() {
        return accountStatus;
    }

    public Integer getIsPremium() {
        return isPremium;
    }

    @NonNull
    public String getUserName() {
        return userName;
    }

    public void setUserName(@NonNull String userName) {
        this.userName = userName;
    }

    @NonNull
    @Ignore
    @Override
    public String toString() {
        return "UserStatusTable{" +
                ", isPremium=" + isPremium +
                ", accountStatus=" + accountStatus +
                '}';
    }
}
