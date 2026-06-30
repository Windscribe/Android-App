package com.windscribe.vpn.localdatabase.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excluded_ip_domain")
data class ExcludedIpDomain(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "type")
    val type: EntryType,
    @ColumnInfo(name = "resolved_ips")
    val resolvedIps: String? = null,
    @ColumnInfo(name = "last_resolved_at")
    val lastResolvedAt: Long? = null,
    @ColumnInfo(name = "resolution_error")
    val resolutionError: String? = null,
) {
    enum class EntryType {
        IP,
        IP_RANGE,
        HOSTNAME,
    }
}
