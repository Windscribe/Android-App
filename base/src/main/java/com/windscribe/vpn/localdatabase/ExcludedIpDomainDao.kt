package com.windscribe.vpn.localdatabase

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Update
    suspend fun update(entry: ExcludedIpDomain)

    @Query("SELECT COUNT(*) FROM excluded_ip_domain WHERE LOWER(value) = LOWER(:value)")
    suspend fun exists(value: String): Int

    @Delete
    suspend fun delete(entry: ExcludedIpDomain)

    @Query("DELETE FROM excluded_ip_domain")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE excluded_ip_domain
        SET resolved_ips = :resolvedIps,
            last_resolved_at = :timestamp,
            resolution_error = :error
        WHERE id = :id
        """,
    )
    suspend fun updateResolvedData(
        id: Long,
        resolvedIps: String?,
        timestamp: Long?,
        error: String?,
    )

    @Query(
        """
        SELECT * FROM excluded_ip_domain
        WHERE type = 'HOSTNAME'
        AND (last_resolved_at IS NULL OR last_resolved_at < :staleTimestamp)
        """,
    )
    suspend fun getStaleHostnames(staleTimestamp: Long): List<ExcludedIpDomain>

    @Query("SELECT * FROM excluded_ip_domain WHERE type = 'HOSTNAME'")
    suspend fun getAllHostnames(): List<ExcludedIpDomain>
}
