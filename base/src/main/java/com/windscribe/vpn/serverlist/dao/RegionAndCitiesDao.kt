/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.windscribe.vpn.serverlist.entity.RegionAndCities

@Dao
interface RegionAndCitiesDao {
    @Transaction
    @Query("select * from Region")
    suspend fun getAllRegionAsync(): List<RegionAndCities>

    @Transaction
    @Query("select * from Region where region_id = :id")
    suspend fun getRegionAsync(id: Int): RegionAndCities
}