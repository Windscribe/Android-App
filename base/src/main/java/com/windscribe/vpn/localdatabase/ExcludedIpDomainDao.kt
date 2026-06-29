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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: ExcludedIpDomain): Long

    @Query("SELECT COUNT(*) FROM excluded_ip_domain WHERE LOWER(value) = LOWER(:value)")
    suspend fun exists(value: String): Int

    @Delete
    suspend fun delete(entry: ExcludedIpDomain)

    @Query("DELETE FROM excluded_ip_domain")
    suspend fun deleteAll()
}
