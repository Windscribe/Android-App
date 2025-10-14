/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.ConfigFile
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ConfigFileDao {
    @Insert(onConflict = REPLACE)
    abstract fun addConfigSync(configFile: ConfigFile)

    @Query("Delete from ConfigFile where primary_key=:id")
    abstract fun deleteCustomConfig(id: Int)

    @Query("Select * from ConfigFile order by primary_key")
    abstract suspend fun getAllConfigs(): List<ConfigFile>

    @get:Query("Select * from ConfigFile order by primary_key")
    abstract val allConfigsAsFlow: Flow<List<ConfigFile>>

    @Query("Select * from ConfigFile where primary_key=:id")
    abstract suspend fun getConfigFileAsync(id: Int): ConfigFile

    @get:Query("Select Max(primary_key) from ConfigFile")
    abstract val maxPrimaryKeySync: Int?

    @Query("Delete from ConfigFile")
    abstract fun clean()
}