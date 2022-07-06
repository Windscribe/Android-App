/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public interface ServerStatusDao {

    @Query("SELECT * FROM server_status_update WHERE user_name =:username")
    Single<ServerStatusUpdateTable> getServerStatus(String username);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrUpdateStatus(ServerStatusUpdateTable serverStatusUpdateTable);


}
