/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.windscribe.vpn.serverlist.entity.RegionAndCities
import io.reactivex.Single

@Dao
interface RegionAndCitiesDao {
    @get:Query("select * from Region")
    val allRegion: Single<List<RegionAndCities>>

    @Transaction
    @Query("select * from Region where region_id = :id")
    fun getRegion(id: Int): Single<RegionAndCities>
}