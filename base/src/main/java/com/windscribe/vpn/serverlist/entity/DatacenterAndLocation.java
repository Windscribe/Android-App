/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.annotation.Keep;
import androidx.room.Embedded;
import androidx.room.Relation;

@Keep
public class DatacenterAndLocation {

    @Embedded
    private Datacenter datacenter;

    @Relation(parentColumn = "region_id", entityColumn = "region_id", entity = Location.class)
    private Location location;

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

}
