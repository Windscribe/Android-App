/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.Server
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ServerDao {

    @Insert(onConflict = REPLACE)
    abstract suspend fun addAll(servers: List<Server>)

    @Insert(onConflict = REPLACE)
    abstract suspend fun addServer(server: Server)

    suspend fun addServers(servers: List<Server>) {
        addAll(servers)
    }

    @Query("DELETE FROM Server WHERE server_id IN (:serverIds)")
    abstract suspend fun deleteByIds(serverIds: List<Int>)

    @Query("DELETE FROM Server")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM Server WHERE datacenter_id = :datacenterId ORDER BY weight DESC")
    abstract suspend fun getServersByDatacenter(datacenterId: Int): List<Server>

    @Query("SELECT * FROM Server WHERE server_id = :serverId")
    abstract suspend fun getServerById(serverId: Int): Server?

    @Query("SELECT * FROM Server ORDER BY datacenter_id, weight DESC")
    abstract suspend fun getAllServers(): List<Server>

    @Query("SELECT * FROM Server ORDER BY datacenter_id, weight DESC")
    abstract fun observeAllServers(): Flow<List<Server>>

    @Query("SELECT COUNT(*) FROM Server")
    abstract suspend fun getServerCount(): Int
}