package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.windscribe.vpn.localdatabase.tables.ExcludedIpDomain
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcludedIpDomainDao {
    @Query("SELECT * FROM excluded_ip_domain")
    fun getAllFlow(): Flow<List<ExcludedIpDomain>>

    @Query("SELECT * FROM excluded_ip_domain")
    suspend fun getAll(): List<ExcludedIpDomain>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExcludedIpDomain): Long

    @Delete
    suspend fun delete(entry: ExcludedIpDomain)

    @Query("DELETE FROM excluded_ip_domain")
    suspend fun deleteAll()
}
