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

    @Ignore
    public Favourite(int id) {
        this.Id = id;
    }

    public Favourite() {

    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }
}
