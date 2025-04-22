/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.windscribe.vpn.serverlist.entity.City;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
abstract public class CityDao {

    @Insert(onConflict = REPLACE)
    public abstract Completable addAll(List<City> cities);

    public Completable addCities(List<City> cities) {
        return deleteAll().andThen(addAll(cities));
    }

    @Query("Delete from City")
    public abstract Completable deleteAll();

    @Query("Select * from City where region_id=:id")
    public abstract Single<List<City>> getAllCities(int id);

    @Query("Select * from City where nodes not null order by primaryKey")
    public abstract Single<List<City>> getCities();

    @Query("Select * from City order by primaryKey")
    public abstract Single<List<City>> getPingableCities();

    @Query("Select * from City where city_id=:id")
    public abstract Single<City> getCityByID(int id);

    @Query("Select * from City where city_id=:id")
    public abstract City getCityByIDAsync(int id);

    @Query("Select * from City where city_id in (:ids)")
    public abstract Single<List<City>> getCityByID(int[] ids);

    @Query("Select gps from City where region_id=:regionId limit 1")
    public abstract Single<String> getCordsByRegionId(int regionId);
}
