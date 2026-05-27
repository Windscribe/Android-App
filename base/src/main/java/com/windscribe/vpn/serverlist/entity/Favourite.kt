/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Keep
@Entity
class Favourite {

    @PrimaryKey
    @ColumnInfo(name = "favourite_id")
    var id: Int = 0

    @ColumnInfo(name = "pinned_ip")
    var pinnedIp: String? = null

    @ColumnInfo(name = "pinned_node_ip")
    var pinnedNodeIp: String? = null

    @Ignore
    constructor(id: Int) {
        this.id = id
        this.pinnedIp = null
        this.pinnedNodeIp = null
    }

    @Ignore
    constructor(id: Int, pinnedIp: String?) {
        this.id = id
        this.pinnedIp = pinnedIp
        this.pinnedNodeIp = null
    }

    @Ignore
    constructor(id: Int, pinnedIp: String?, pinnedNodeIp: String?) {
        this.id = id
        this.pinnedIp = pinnedIp
        this.pinnedNodeIp = pinnedNodeIp
    }

    constructor()
}
