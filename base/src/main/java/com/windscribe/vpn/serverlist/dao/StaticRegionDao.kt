/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.StaticRegion

@Dao
abstract class StaticRegionDao {

    suspend fun addStaticRegions(staticRegions: List<StaticRegion>) {
        deleteAll()
        insert(staticRegions)
    }

    @Query("Select * from StaticRegion")
    abstract suspend fun getAllStaticRegions(): List<StaticRegion>

    @Query("Select * from staticregion where id =:id")
    abstract suspend fun getStaticRegionByIDAsync(id: Int): StaticRegion?

    @Query("Delete from StaticRegion")
    abstract suspend fun deleteAll()

    @Insert(onConflict = REPLACE)
    abstract suspend fun insert(staticRegions: List<StaticRegion>)

    @Query("Delete from StaticRegion")
    abstract fun clean()
}