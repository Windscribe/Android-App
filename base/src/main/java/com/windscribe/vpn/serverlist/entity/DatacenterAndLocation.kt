/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Relation

@Keep
class DatacenterAndLocation {
    @Embedded
    var datacenter: Datacenter = Datacenter()

    @Relation(parentColumn = "region_id", entityColumn = "region_id", entity = Location::class)
    var location: Location? = null
}
