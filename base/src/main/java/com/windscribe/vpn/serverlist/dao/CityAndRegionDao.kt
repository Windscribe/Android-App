/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.windscribe.vpn.serverlist.entity.CityAndRegion

@Dao
interface CityAndRegionDao {

    @Transaction
    @Query("Select * from City where city_id=:cityId limit 1")
    fun getCityAndRegion(cityId: Int): CityAndRegion

    @Query("Select region_id From city where city_id=:cityId limit 1")
    suspend fun getRegionIdFromCityAsync(cityId: Int): Int
}