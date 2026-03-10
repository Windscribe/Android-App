/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.Location

@Dao
abstract class LocationDao {

    @Insert(onConflict = REPLACE)
    abstract suspend fun addAll(regions: List<Location>)

    suspend fun addRegions(regions: List<Location>) {
        deleteAll()
        addAll(regions)
    }

    @Query("Delete from Location")
    abstract suspend fun deleteAll()

    @Query("Delete from Location")
    abstract fun clean()

    @Query("Select * from Location where region_id=:regionId")
    abstract suspend fun getRegionById(regionId: Int): Location
}