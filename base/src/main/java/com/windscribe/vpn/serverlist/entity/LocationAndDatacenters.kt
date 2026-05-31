/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import java.util.Objects

@Keep
class LocationAndDatacenters {
    @Relation(parentColumn = "region_id", entityColumn = "region_id", entity = Datacenter::class)
    var datacenters: List<Datacenter> = emptyList()

    @Ignore
    var latencyTotal: Int = 0

    @Embedded
    var location: Location? = null

    @Ignore
    var isExpanded: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocationAndDatacenters) return false
        return latencyTotal == other.latencyTotal &&
            datacenters == other.datacenters &&
            location == other.location &&
            isExpanded == other.isExpanded
    }

    override fun hashCode(): Int = Objects.hash(datacenters, latencyTotal, location, isExpanded)
}
