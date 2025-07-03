/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.windscribe.vpn.serverlist.entity.StaticRegion;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
abstract public class StaticRegionDao {

    public Completable addStaticRegions(List<StaticRegion> staticRegions) {
        return deleteAll().andThen(insert(staticRegions));
    }

    @Query("Select * from StaticRegion")
    public abstract Single<List<StaticRegion>> getAllStaticRegions();

    @Query("Select * from StaticRegion")
    public abstract Flowable<List<StaticRegion>> getAllStaticRegionsFlowAble();

    @Query("Select * from staticregion where id =:id")
    public abstract Single<StaticRegion> getStaticRegionByID(int id);

    @Query("SELECT Count(*) FROM staticregion")
    public abstract Single<Integer> getStaticRegionCount();

    @Query("Delete from StaticRegion")
    abstract Completable deleteAll();

    @Insert(onConflict = REPLACE)
    abstract CompletableSource insert(List<StaticRegion> staticRegions);

    @Query("Delete from StaticRegion")
    public abstract void clean();
}
