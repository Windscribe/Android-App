/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.windscribe.vpn.localdatabase.tables.WindNotification
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface WindNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(newsFeedNotification: List<WindNotification>): Completable

    @Query("Select * from WindNotification order by date DESC")
    fun getWindNotifications(): Single<List<WindNotification>>

    @Query("Delete from WindNotification")
    abstract fun clean()
}