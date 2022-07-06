/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase;

import androidx.room.Dao;
import androidx.room.Query;

import com.windscribe.vpn.localdatabase.tables.PingTestResults;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface PingTestDao {

    @Query("SELECT * FROM ping_results")
    Single<List<PingTestResults>> getPingList();

}
