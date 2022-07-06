/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.List;

public class RegionAndCities {

    @Relation(parentColumn = "region_id", entityColumn = "region_id", entity = City.class)
    private List<City> cities;

    @Ignore
    private int latencyTotal;

    @Embedded
    private Region region;

    public List<City> getCities() {
        return cities;
    }

    public void setCities(List<City> cities) {
        this.cities = cities;
    }

    @Ignore
    public int getLatencyTotal() {
        return latencyTotal;
    }

    @Ignore
    public void setLatencyTotal(int latencyTotal) {
        this.latencyTotal = latencyTotal;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }
}
