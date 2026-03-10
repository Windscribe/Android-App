/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.windscribe.vpn.serverlist.entity.LocationAndDatacenters

@Dao
interface LocationAndDatacentersDao {
    @Transaction
    @Query("select * from Location")
    suspend fun getAllLocationsAsync(): List<LocationAndDatacenters>

    @Transaction
    @Query("select * from Location where region_id = :id")
    suspend fun getLocationAsync(id: Int): LocationAndDatacenters
}