/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

@SuppressWarnings("unused")
public class CityDetails {

    @Embedded
    private City city;

    @Relation(parentColumn = "city_id", entityColumn = "favourite_id", entity = Favourite.class)
    private Favourite favourite;

    @Relation(parentColumn = "city_id", entityColumn = "ping_id", entity = PingTime.class)
    private PingTime pingTime;

    @Relation(parentColumn = "region_id", entityColumn = "region_id", entity = Region.class)
    private Region region;

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public Favourite getFavourite() {
        return favourite;
    }

    public void setFavourite(Favourite favourite) {
        this.favourite = favourite;
    }

    public PingTime getPingTime() {
        return pingTime;
    }

    public void setPingTime(PingTime pingTime) {
        this.pingTime = pingTime;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }
}
