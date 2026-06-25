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
) {
    enum class EntryType {
        IP,
        IP_RANGE,
        HOSTNAME,
    }
}
