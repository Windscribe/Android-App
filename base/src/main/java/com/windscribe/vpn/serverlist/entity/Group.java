/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import java.util.List;

public class Group extends ExpandableGroup<City> {

    private final int latencyAverage;

    private final Region region;

    public Group(String title, Region region, List<City> cities, int latencyAverage) {
        super(title, cities);
        this.region = region;
        this.latencyAverage = latencyAverage;
    }

    public int getLatencyAverage() {
        return latencyAverage;
    }

    public Region getRegion() {
        return region;
    }
}
