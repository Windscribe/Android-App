/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

public class CityAndRegion {

    @Embedded
    private City city;

    @Relation(parentColumn = "region_id", entityColumn = "region_id", entity = Region.class)
    private Region region;

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

}
