/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.windscribe.vpn.localdatabase.tables.UserStatusTable;

import io.reactivex.Completable;
import io.reactivex.Flowable;

@Dao
public interface UserStatusDao {

    @Query("Delete from user_account_info")
    Completable delete();

    @Query("SELECT * FROM user_account_info WHERE user_name =:username")
    Flowable<UserStatusTable> getUserStatusTable(String username);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateUserStatus(UserStatusTable userStatusTable);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable update(UserStatusTable userStatusTable);

    @Query("Delete from user_account_info")
    abstract void clean();

}
