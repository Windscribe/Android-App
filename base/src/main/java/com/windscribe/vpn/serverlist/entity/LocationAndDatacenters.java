/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.annotation.Keep;
import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.List;
import java.util.Objects;

@Keep
public class LocationAndDatacenters {

    @Relation(parentColumn = "region_id", entityColumn = "region_id", entity = Datacenter.class)
    private List<Datacenter> datacenters;

    @Ignore
    private int latencyTotal;

    @Embedded
    private Location location;

    public List<Datacenter> getDatacenters() {
        return datacenters;
    }

    public void setDatacenters(List<Datacenter> datacenters) {
        this.datacenters = datacenters;
    }

    @Ignore
    public int getLatencyTotal() {
        return latencyTotal;
    }

    @Ignore
    public void setLatencyTotal(int latencyTotal) {
        this.latencyTotal = latencyTotal;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Ignore
    public Boolean isExpanded = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationAndDatacenters)) return false;
        LocationAndDatacenters that = (LocationAndDatacenters) o;
        return latencyTotal == that.latencyTotal && datacenters.equals(that.datacenters) && location.equals(that.location) && isExpanded.equals(that.isExpanded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datacenters, latencyTotal, location, isExpanded);
    }
}
