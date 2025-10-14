/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable

@Dao
interface ServerStatusDao {

    @Query("SELECT * FROM server_status_update WHERE user_name =:username")
    suspend fun getServerStatus(username: String): ServerStatusUpdateTable

    @Insert(onConflict = REPLACE)
    suspend fun insertOrUpdateStatus(serverStatusUpdateTable: ServerStatusUpdateTable)

    @Insert(onConflict = REPLACE)
    suspend fun insertOrUpdateStatusAsync(serverStatusUpdateTable: ServerStatusUpdateTable)

    @Query("Delete from server_status_update")
    fun clean()
}
