package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.windscribe.vpn.localdatabase.tables.UnBlockWgParam
import kotlinx.coroutines.flow.Flow

@Dao
interface UnblockWgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unblockWgParams: List<UnBlockWgParam>)

    @Query("Delete from UnBlockWgParam")
    suspend fun deleteAll()

    @Query("Select * from UnBlockWgParam")
    fun getUnblockWgParams(): Flow<List<UnBlockWgParam>>
}