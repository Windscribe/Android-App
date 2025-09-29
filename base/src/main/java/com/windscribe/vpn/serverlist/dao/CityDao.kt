/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.City
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class CityDao {

    @Insert(onConflict = REPLACE)
    abstract fun addAll(cities: List<City>): Completable

    fun addCities(cities: List<City>): Completable {
        return deleteAll().andThen(addAll(cities))
    }

    @Query("Delete from City")
    abstract fun deleteAll(): Completable

    @Query("Select * from City where region_id=:id")
    abstract fun getAllCities(id: Int): Single<List<City>>

    @get:Query("Select * from City where nodes not null order by primaryKey")
    abstract val cities: Single<List<City>>

    @Query("Select * from City where nodes not null order by primaryKey")
    abstract suspend fun getCitiesAsync(): List<City>

    @get:Query("Select * from City order by primaryKey")
    abstract val pingableCities: Single<List<City>>

    @Query("Select * from City where city_id=:id")
    abstract fun getCityByID(id: Int): Single<City>

    @Query("Select * from City where city_id=:id")
    abstract fun getCityByIDAsync(id: Int): City

    @Query("Select * from City where city_id in (:ids)")
    abstract fun getCityByID(ids: IntArray?): Single<List<City>>

    @Query("Select gps from City where region_id=:regionId limit 1")
    abstract fun getCordsByRegionId(regionId: Int): Single<String>

    @Query("Delete from City")
    abstract fun clean()
}