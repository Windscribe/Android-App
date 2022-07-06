/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.serverlist.entity.PingTime;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
abstract public class PingTimeDao {


    public Completable addPingTime(PingTime pingTime) {
        return addPing(pingTime);
    }

    @Query("Select * from PingTime")
    public abstract Single<List<PingTime>> getAllPings();

    @Query("Select ping_id from PingTime where ping_time =:pingTime and isPro=:pro")
    public abstract Single<Integer> getFreePingIdFromTime(boolean pro, int pingTime);

    @Query("Select MIN(ping_time) from PingTime where ping_time!= -1 and static=0 limit 1")
    public abstract Single<Integer> getLowestPing();

    @Query("Select MIN(ping_time) from PingTime where ping_time!= -1 and isPro=:pro and static=0 limit 1")
    public abstract Single<Integer> getLowestPingForFreeUser(boolean pro);

    public Single<Integer> getLowestPingId() {
        boolean freeUser = Windscribe.getAppContext().getPreference().getUserStatus() == 0;
        if (freeUser) {
            return getLowestPingForFreeUser(false).flatMap(time -> getFreePingIdFromTime(false, time));
        } else {
            return getLowestPing().flatMap(this::getPingIdFromTime);
        }
    }

    @Query("Select ping_id from PingTime where ping_time =:pingTime")
    public abstract Single<Integer> getPingIdFromTime(int pingTime);

    @Insert(onConflict = REPLACE)
    abstract Completable addPing(PingTime pingTime);
}
