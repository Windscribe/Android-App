/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.City

@Dao
abstract class CityDao {

    @Insert(onConflict = REPLACE)
    abstract suspend fun addAll(cities: List<City>)

    suspend fun addCities(cities: List<City>) {
        deleteAll()
        addAll(cities)
    }

    @Query("Delete from City")
    abstract suspend fun deleteAll()

    @Query("Select * from City where region_id=:id")
    abstract suspend fun getAllCitiesAsync(id: Int): List<City>

    @Query("Select * from City where nodes not null order by primaryKey")
    abstract suspend fun getCitiesAsync(): List<City>

    @Query("Select * from City order by primaryKey")
    abstract suspend fun getPingableCities(): List<City>

    @Query("Select * from City where city_id=:id")
    abstract fun getCityByIDAsync(id: Int): City

    @Query("Select * from City where city_id in (:ids)")
    abstract suspend fun getCityByID(ids: IntArray?): List<City>

    @Query("Delete from City")
    abstract fun clean()
}