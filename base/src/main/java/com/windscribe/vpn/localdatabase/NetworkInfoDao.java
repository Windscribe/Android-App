/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.windscribe.vpn.localdatabase.tables.NetworkInfo;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import kotlinx.coroutines.flow.Flow;

@Dao
public abstract class NetworkInfoDao {

    public Single<Long> addNetwork(NetworkInfo networkInfo) {
        return insert(networkInfo);
    }

    @Query("Delete from Network_Info where networkName=:networkName")
    public abstract Single<Integer> delete(String networkName);

    @Query("Select * from network_info")
    public abstract Flowable<List<NetworkInfo>> getAllNetworksWithUpdate();

    @Query("Select * from network_info")
    public abstract Flow<List<NetworkInfo>> allNetworks();

    @Query("Select * from network_info where networkName=:networkName")
    public abstract Single<NetworkInfo> getNetwork(String networkName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract Single<Long> insert(NetworkInfo networkInfo);

    @Update
    public abstract Single<Integer> updateNetwork(NetworkInfo networkInfo);

    @Update
    public abstract int updateNetworkSync(NetworkInfo networkInfo);

    @Query("Delete from Network_Info where networkName=:networkName")
    public abstract int deleteNetworkSync(String networkName);
}
