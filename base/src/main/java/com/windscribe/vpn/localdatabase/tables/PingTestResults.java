/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@SuppressWarnings("unused")
@Entity(tableName = "ping_results")
public class PingTestResults {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "node_name")
    private String mNodeName;


    @ColumnInfo(name = "node_parent_index")
    private final int mNodeParentIndex;

    @ColumnInfo(name = "node_ping_time")
    private final Integer mNodePingTime;


    public PingTestResults(@NonNull String mNodeName, int mNodeParentIndex, Integer mNodePingTime) {
        this.mNodeName = mNodeName;
        this.mNodeParentIndex = mNodeParentIndex;
        this.mNodePingTime = mNodePingTime;
    }

    @NonNull
    public String getNodeName() {
        return mNodeName;
    }

    public void setNodeName(@NonNull String mNodeName) {
        this.mNodeName = mNodeName;
    }

    public int getNodeParentIndex() {
        return mNodeParentIndex;
    }

    public Integer getNodePingTime() {
        return mNodePingTime;
    }
}
