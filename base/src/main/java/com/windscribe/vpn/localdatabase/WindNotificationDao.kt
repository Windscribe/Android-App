/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.windscribe.vpn.localdatabase.tables.WindNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface WindNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(newsFeedNotification: List<WindNotification>)

    @Query("Select * from WindNotification order by date DESC")
    suspend fun getWindNotifications(): List<WindNotification>

    @Query("Select * from WindNotification order by date DESC")
    suspend fun getWindNotificationsAsync(): List<WindNotification>

    @Query("Select * from WindNotification order by date DESC")
    fun observeNotifications(): Flow<List<WindNotification>>

    @Query("Delete from WindNotification")
    suspend fun cleanAsync()

    @Query("Delete from WindNotification")
    fun clean()

    @Query("SELECT isRead FROM WindNotification WHERE id = :notificationId")
    suspend fun isRead(notificationId: Int): Boolean?

    @Query("UPDATE WindNotification SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int)
}