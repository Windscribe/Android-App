/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@Keep
@Entity(tableName = "Location", indices = {@Index(value = {"region_id"},
        unique = true)})
public class Location {

    @SerializedName("groups")
    @Expose(serialize = false, deserialize = true)
    @Ignore
    public List<Datacenter> datacenters;

    @PrimaryKey(autoGenerate = true)
    public int primaryKey;

    @SerializedName("country_code")
    @Expose
    @ColumnInfo(name = "country_code")
    private String countryCode;

    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "region_id")
    private int id;

    @SerializedName("name")
    @Expose
    @ColumnInfo(name = "name")
    private String name;

    @SerializedName("short_name")
    @Expose
    @ColumnInfo(name = "short_name")
    private String shortName;

    @SerializedName("sort_order")
    @Expose
    @ColumnInfo(name = "sort_order")
    private int sortOrder;

    @SerializedName("continent")
    @Expose
    @ColumnInfo(name = "continent")
    private String continent;

    public Location(int id, String name, String countryCode, String shortName, int sortOrder, String continent) {
        this.id = id;
        this.name = name;
        this.countryCode = countryCode;
        this.shortName = shortName;
        this.sortOrder = sortOrder;
        this.continent = continent;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Location) {
            Location region = (Location) obj;
            return region.getId() == id;
        }
        return false;
    }

    public List<Datacenter> getDatacenters() {
        return datacenters;
    }

    public void setDatacenters(List<Datacenter> datacenters) {
        this.datacenters = datacenters;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(int primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    @NonNull
    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", shortName='" + shortName + '\'' +
                ", sortOrder=" + sortOrder +
                ", continent='" + continent + '\'' +
                '}';
    }
}
