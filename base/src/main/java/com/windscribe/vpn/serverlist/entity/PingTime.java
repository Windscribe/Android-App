/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Keep
@Entity
public class PingTime {

    @ColumnInfo(name = "static")
    public boolean isStatic;

    @ColumnInfo(name = "ping_time")
    public int pingTime;

    @PrimaryKey
    @ColumnInfo(name = "ping_id")
    public int ping_id;

    @ColumnInfo(name = "isPro")
    public boolean pro;

    @ColumnInfo(name = "region_id")
    public int regionId;

    @ColumnInfo(name = "updated_at")
    public long updatedAt = System.currentTimeMillis();

    @ColumnInfo(name = "ip")
    public String ip = "";

    public int getId() {
        return ping_id;
    }

    public void setId(int id) {
        this.ping_id = id;
    }

    public int getPingTime() {
        return pingTime;
    }

    public void setPingTime(int pingTime) {
        this.pingTime = pingTime;
    }

    public int getPing_id() {
        return ping_id;
    }

    public void setPing_id(int ping_id) {
        this.ping_id = ping_id;
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public boolean isPro() {
        return pro;
    }

    public void setPro(boolean pro) {
        this.pro = pro;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public void setIp(String ip) { this.ip = ip; }

    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @NonNull
    @Ignore
    @Override
    public String toString() {
        return "PingTime{" +
                "ping_id=" + ping_id +
                ", pingTime=" + pingTime +
                ", regionId=" + regionId +
                ", isStatic=" + isStatic +
                ", pro=" + pro +
                '}';
    }
}
