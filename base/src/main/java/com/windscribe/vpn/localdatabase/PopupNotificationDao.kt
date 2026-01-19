/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import kotlinx.coroutines.flow.Flow

@Dao
interface PopupNotificationDao {

    @Query("SELECT * FROM notification_table WHERE user_name =:userName")
    fun getPopupNotificationAsFlow(userName: String): Flow<List<PopupNotificationTable>>

    @Insert(onConflict = REPLACE)
    fun insertPopupNotification(popupNotificationTable: PopupNotificationTable)

    @Query("UPDATE notification_table SET popup_status = 0 WHERE notification_id = :notificationId")
    fun markPopupAsShown(notificationId: Int)

    @Query("SELECT notification_id FROM notification_table WHERE notification_id = :notificationId LIMIT 1")
    suspend fun getPopupNotificationId(notificationId: Int): Int?

    @Query("Delete from notification_table")
    fun clean()
}
