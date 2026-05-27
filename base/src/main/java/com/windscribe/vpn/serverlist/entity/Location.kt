/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "Location", indices = [Index(value = ["region_id"], unique = true)])
class Location {
    @SerializedName("groups")
    @Expose(serialize = false, deserialize = true)
    @Ignore
    var datacenters: List<Datacenter>? = null

    @PrimaryKey(autoGenerate = true)
    var primaryKey: Int = 0

    @SerializedName("country_code")
    @Expose
    @ColumnInfo(name = "country_code")
    var countryCode: String? = null

    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "region_id")
    var id: Int = 0

    @SerializedName("name")
    @Expose
    @ColumnInfo(name = "name")
    var name: String? = null

    @SerializedName("short_name")
    @Expose
    @ColumnInfo(name = "short_name")
    var shortName: String? = null

    @SerializedName("sort_order")
    @Expose
    @ColumnInfo(name = "sort_order")
    var sortOrder: Int = 0

    @SerializedName("continent")
    @Expose
    @ColumnInfo(name = "continent")
    var continent: String? = null

    @Ignore
    constructor(
        id: Int,
        name: String?,
        countryCode: String?,
        shortName: String?,
        sortOrder: Int,
        continent: String?,
    ) {
        this.id = id
        this.name = name
        this.countryCode = countryCode
        this.shortName = shortName
        this.sortOrder = sortOrder
        this.continent = continent
    }

    constructor()

    override fun equals(obj: Any?): Boolean {
        if (obj is Location) {
            return obj.id == id
        }
        return false
    }

    override fun toString(): String =
        "Location{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", countryCode='" + countryCode + '\'' +
            ", shortName='" + shortName + '\'' +
            ", sortOrder=" + sortOrder +
            ", continent='" + continent + '\'' +
            '}'
}
