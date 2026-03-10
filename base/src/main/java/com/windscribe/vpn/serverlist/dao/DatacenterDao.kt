/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.Datacenter

@Dao
abstract class DatacenterDao {

    @Insert(onConflict = REPLACE)
    abstract suspend fun addAll(cities: List<Datacenter>)

    suspend fun addCities(cities: List<Datacenter>) {
        deleteAll()
        addAll(cities)
    }

    @Query("Delete from Datacenter")
    abstract suspend fun deleteAll()

    @Query("Select * from Datacenter where region_id=:id")
    abstract suspend fun getAllCitiesAsync(id: Int): List<Datacenter>

    // V2: Nodes column removed, all cities are valid
    @Query("Select * from Datacenter order by primaryKey")
    abstract suspend fun getCitiesAsync(): List<Datacenter>

    @Query("Select * from Datacenter order by primaryKey")
    abstract suspend fun getPingableCities(): List<Datacenter>

    @Query("Select * from Datacenter where city_id=:id")
    abstract fun getCityByIDAsync(id: Int): Datacenter

    @Query("Select * from Datacenter where city_id in (:ids)")
    abstract suspend fun getCityByID(ids: IntArray?): List<Datacenter>

    @Query("Delete from Datacenter")
    abstract fun clean()
}