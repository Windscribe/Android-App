/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.annotation.Keep;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Keep
@Entity
public class Favourite {

    @PrimaryKey
    @ColumnInfo(name = "favourite_id")
    private int Id;

    @ColumnInfo(name = "pinned_ip")
    private String pinnedIp;

    @ColumnInfo(name = "pinned_node_ip")
    private String pinnedNodeIp;

    @Ignore
    public Favourite(int id) {
        this.Id = id;
        this.pinnedIp = null;
        this.pinnedNodeIp = null;
    }

    @Ignore
    public Favourite(int id, String pinnedIp) {
        this.Id = id;
        this.pinnedIp = pinnedIp;
        this.pinnedNodeIp = null;
    }

    @Ignore
    public Favourite(int id, String pinnedIp, String pinnedNodeIp) {
        this.Id = id;
        this.pinnedIp = pinnedIp;
        this.pinnedNodeIp = pinnedNodeIp;
    }

    public Favourite() {

    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getPinnedIp() {
        return pinnedIp;
    }

    public void setPinnedIp(String pinnedIp) {
        this.pinnedIp = pinnedIp;
    }

    public String getPinnedNodeIp() {
        return pinnedNodeIp;
    }

    public void setPinnedNodeIp(String pinnedNodeIp) {
        this.pinnedNodeIp = pinnedNodeIp;
    }
}
