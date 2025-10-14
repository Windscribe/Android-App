/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.serverlist.entity.PingTime
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PingTimeDao {

    @Query("Select * from PingTime")
    abstract suspend fun getAllPingsAsync(): List<PingTime>

    @get:Query("Select * from PingTime")
    abstract val allPingsAsStateFlow: Flow<List<PingTime>>

    suspend fun getLowestPingIdAsync(): Int {
        val freeUser = Windscribe.appContext.preference.userStatus == 0
        return if (freeUser) {
            val time = getLowestPingForFreeUserAsync(false)
            getFreePingIdFromTimeAsync(false, time)
        } else {
            val time = getLowestPingAsync()
            getPingIdFromTimeAsync(time)
        }
    }

    @Query("Select MIN(ping_time) from PingTime where ping_time!= -1 and static=0 limit 1")
    abstract suspend fun getLowestPingAsync(): Int

    @Query("Select MIN(ping_time) from PingTime where ping_time!= -1 and isPro=:pro and static=0 limit 1")
    abstract suspend fun getLowestPingForFreeUserAsync(pro: Boolean): Int

    @Query("Select ping_id from PingTime where ping_time =:pingTime and isPro=:pro")
    abstract suspend fun getFreePingIdFromTimeAsync(pro: Boolean, pingTime: Int): Int

    @Query("Select ping_id from PingTime where ping_time =:pingTime")
    abstract suspend fun getPingIdFromTimeAsync(pingTime: Int): Int

    @Insert(onConflict = REPLACE)
    abstract suspend fun addPing(pingTime: PingTime)
}