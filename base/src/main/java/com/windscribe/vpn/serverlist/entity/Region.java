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
@Entity(tableName = "Region", indices = {@Index(value = {"region_id"},
        unique = true)})
public class Region {

    @SerializedName("groups")
    @Expose
    @Ignore
    public List<City> cities;

    @PrimaryKey(autoGenerate = true)
    public int primaryKey;

    @SerializedName("country_code")
    @Expose
    @ColumnInfo(name = "country_code")
    private String countryCode;

    @SerializedName("dns_hostname")
    @Expose
    @ColumnInfo(name = "dns_host_name")
    private String dnsHostName;

    @SerializedName("force_expand")
    @Expose
    @ColumnInfo(name = "force_expand")
    private int forceExpand;

    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "region_id")
    private int id;

    @SerializedName("loc_type")
    @Expose
    @ColumnInfo(name = "loc_type")
    private String locationType;

    @SerializedName("name")
    @Expose
    @ColumnInfo(name = "name")
    private String name;

    @SerializedName("p2p")
    @Expose
    @ColumnInfo(name = "p2p")
    private int p2p;

    @SerializedName("premium_only")
    @Expose
    @ColumnInfo(name = "premium")
    private int premium;

    @SerializedName("short_name")
    @Expose
    @ColumnInfo(name = "short_name")
    private String shortName;

    @SerializedName("status")
    @Expose
    @ColumnInfo(name = "status")
    private int status;

    @SerializedName("tz")
    @Expose
    @ColumnInfo(name = "tz")
    private String tz;

    @SerializedName("tz_offset")
    @Expose
    @ColumnInfo(name = "tz_offset")
    private String tzOffSet;

    public Region(int id, String name, String countryCode, int status, int premium, String shortName, int p2p,
            String tz, String tzOffSet, String locationType, int forceExpand, String dnsHostName) {
        this.id = id;
        this.name = name;
        this.countryCode = countryCode;
        this.status = status;
        this.premium = premium;
        this.shortName = shortName;
        this.p2p = p2p;
        this.tz = tz;
        this.tzOffSet = tzOffSet;
        this.locationType = locationType;
        this.forceExpand = forceExpand;
        this.dnsHostName = dnsHostName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Region) {
            Region region = (Region) obj;
            return region.getId() == id;
        }
        return false;
    }

    public List<City> getCities() {
        return cities;
    }

    public void setCities(List<City> cities) {
        this.cities = cities;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getDnsHostName() {
        return dnsHostName;
    }

    public void setDnsHostName(String dnsHostName) {
        this.dnsHostName = dnsHostName;
    }

    public int getForceExpand() {
        return forceExpand;
    }

    public void setForceExpand(int forceExpand) {
        this.forceExpand = forceExpand;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getP2p() {
        return p2p;
    }

    public void setP2p(int p2p) {
        this.p2p = p2p;
    }

    public int getPremium() {
        return premium;
    }

    public void setPremium(int premium) {
        this.premium = premium;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTz() {
        return tz;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public String getTzOffSet() {
        return tzOffSet;
    }

    public void setTzOffSet(String tzOffSet) {
        this.tzOffSet = tzOffSet;
    }

    @NonNull
    @Override
    public String toString() {
        return "Region{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", premium=" + premium +
                ", shortName='" + shortName + '\'' +
                ", p2p=" + p2p +
                ", tz='" + tz + '\'' +
                ", tzOffSet='" + tzOffSet + '\'' +
                ", locationType='" + locationType + '\'' +
                ", forceExpand=" + forceExpand +
                ", dnsHostName='" + dnsHostName + '\'' +
                '}';
    }
}
