/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.List;
import java.util.Objects;

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

    @Ignore
    public Boolean isExpanded = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionAndCities)) return false;
        RegionAndCities that = (RegionAndCities) o;
        return latencyTotal == that.latencyTotal && cities.equals(that.cities) && region.equals(that.region) && isExpanded.equals(that.isExpanded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cities, latencyTotal, region, isExpanded);
    }
}
