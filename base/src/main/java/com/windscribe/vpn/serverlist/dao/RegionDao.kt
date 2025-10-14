/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.Region

@Dao
abstract class RegionDao {

    @Insert(onConflict = REPLACE)
    abstract suspend fun addAll(regions: List<Region>)

    suspend fun addRegions(regions: List<Region>) {
        deleteAll()
        addAll(regions)
    }

    @Query("Delete from Region")
    abstract suspend fun deleteAll()

    @Query("Delete from Region")
    abstract fun clean()
}