/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.windscribe.vpn.serverlist.entity.ConfigFile;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import kotlinx.coroutines.flow.Flow;

@Dao
abstract public class ConfigFileDao {

    @Insert(onConflict = REPLACE)
    public abstract Completable addConfig(ConfigFile configFile);

    @Insert(onConflict = REPLACE)
    public abstract void addConfigSync(ConfigFile configFile);

    @Query("Delete from ConfigFile where primary_key=:id")
    public abstract Completable delete(int id);

    @Query("Delete from ConfigFile where primary_key=:id")
    public abstract void deleteCustomConfig(int id);

    @Query("Select * from ConfigFile order by primary_key")
    public abstract Single<List<ConfigFile>> getAllConfigs();

    @Query("Select * from ConfigFile order by primary_key")
    public abstract Flow<List<ConfigFile>> getAllConfigsAsFlow();

    @Query("Select * from ConfigFile where primary_key=:id")
    public abstract Single<ConfigFile> getConfigFile(int id);

    @Query("Select Max(primary_key) from ConfigFile")
    public abstract Single<Integer> getMaxPrimaryKey();

    @Query("Select Max(primary_key) from ConfigFile")
    public abstract Integer getMaxPrimaryKeySync();

    @Query("Delete from ConfigFile")
    public abstract void clean();
}
