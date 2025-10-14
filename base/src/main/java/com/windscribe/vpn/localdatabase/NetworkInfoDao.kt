/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NetworkInfoDao {

    suspend fun addNetwork(networkInfo: NetworkInfo): Long {
        return insert(networkInfo)
    }

    @Query("Select * from network_info")
    abstract fun allNetworks(): Flow<List<NetworkInfo>>

    @Query("Select * from network_info where networkName=:networkName")
    abstract fun getNetwork(networkName: String): NetworkInfo?

    @Insert(onConflict = REPLACE)
    abstract suspend fun insert(networkInfo: NetworkInfo): Long

    @Update
    abstract suspend fun updateNetworkSync(networkInfo: NetworkInfo): Int

    @Query("Delete from Network_Info where networkName=:networkName")
    abstract suspend fun deleteNetworkSync(networkName: String): Int

    @Query("Delete from network_info")
    abstract fun clean()
}
