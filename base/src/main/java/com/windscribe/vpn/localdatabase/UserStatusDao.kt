/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.localdatabase.tables.UserStatusTable

@Dao
interface UserStatusDao {

    @Query("Delete from user_account_info")
    suspend fun delete()

    @Insert(onConflict = REPLACE)
    suspend fun insertOrUpdateUserStatus(userStatusTable: UserStatusTable)

    @Query("Delete from user_account_info")
    fun clean()
}
